#include "generator/coastlines_generator.hpp"

#include "generator/feature_builder.hpp"

#include "indexer/cell_id.hpp"
#include "indexer/ftypes_matcher.hpp"

#include "coding/point_coding.hpp"

#include "geometry/region2d/binary_operators.hpp"

#include "base/logging.hpp"

#include <algorithm>
#include <condition_variable>
#include <functional>
#include <list>
#include <memory>
#include <mutex>
#include <thread>
#include <utility>

namespace coastlines_generator
{
using RegionT = m2::RegionI;
using PointT = m2::PointI;
using RectT = m2::RectI;

m2::RectD GetLimitRect(RegionT const & rgn)
{
  RectT const r = rgn.GetRect();
  return m2::RectD(r.minX(), r.minY(), r.maxX(), r.maxY());
}

PointT D2I(m2::PointD const & p)
{
  m2::PointU const pu = PointDToPointU(p, kPointCoordBits);
  return PointT(static_cast<int32_t>(pu.x), static_cast<int32_t>(pu.y));
}

m2::RegionI CreateRegionI(std::vector<m2::PointD> const & poly)
{
  CHECK_GREATER(poly.size(), 2, ());
  CHECK(poly.front().EqualDxDy(poly.back(), 1.0E-12), (poly.front(), poly.back()));

  m2::RegionI rgn;
  for (auto it = poly.begin() + 1; it != poly.end(); ++it)
    rgn.AddPoint(D2I(*it));
  return rgn;
}

class DoAddToTree : public FeatureEmitterIFace
{
  CoastlineFeaturesGenerator & m_rMain;
  size_t m_notMergedCoastsCount;
  size_t m_totalNotMergedCoastsPoints;

public:
  explicit DoAddToTree(CoastlineFeaturesGenerator & rMain)
    : m_rMain(rMain)
    , m_notMergedCoastsCount(0)
    , m_totalNotMergedCoastsPoints(0)
  {}

  virtual void operator()(feature::FeatureBuilder const & fb)
  {
    if (fb.IsGeometryClosed())
      m_rMain.AddRegionToTree(fb);
    else
    {
      base::GeoObjectId const firstWay = fb.GetFirstOsmId();
      base::GeoObjectId const lastWay = fb.GetLastOsmId();
      if (firstWay == lastWay)
        LOG(LINFO, ("Not merged coastline, way", firstWay.GetSerialId(), "(", fb.GetPointsCount(), "points)"));
      else
        LOG(LINFO, ("Not merged coastline, ways", firstWay.GetSerialId(), "to", lastWay.GetSerialId(), "(",
                    fb.GetPointsCount(), "points)"));
      ++m_notMergedCoastsCount;
      m_totalNotMergedCoastsPoints += fb.GetPointsCount();
    }
  }

  bool HasNotMergedCoasts() const { return m_notMergedCoastsCount != 0; }

  size_t GetNotMergedCoastsCount() const { return m_notMergedCoastsCount; }

  size_t GetNotMergedCoastsPoints() const { return m_totalNotMergedCoastsPoints; }
};

class DoDifference
{
  RegionT m_cell;
  std::vector<RegionT> m_regions;

public:
  explicit DoDifference(RegionT const & rgn) : m_cell(rgn) {}

  void operator()(RegionT const & r)
  {
    auto const & bounds = m_cell.GetRect();
    auto const & regionBounds = r.GetRect();
    // Match the spatial index's strict overlap check, excluding shared edges.
    if (bounds.maxX() <= regionBounds.minX() || bounds.minX() >= regionBounds.maxX() ||
        bounds.maxY() <= regionBounds.minY() || bounds.minY() >= regionBounds.maxY())
      return;

    // if r is fully inside source rect region,
    // put it to the result vector without any intersection
    if (bounds.IsRectInside(regionBounds))
      m_regions.push_back(r);
    else
      m2::IntersectRegions(m_cell, r, m_regions);
  }

  size_t GetPointsCount() const
  {
    size_t count = m_cell.GetPointsCount();
    for (auto const & region : m_regions)
      count += region.GetPointsCount();
    return count;
  }

  std::vector<RegionT> TakeRegions() { return std::move(m_regions); }

  void AssignGeometry(feature::FeatureBuilder & fb)
  {
    auto const addPolygon = [&fb](RegionT const & region)
    {
      std::vector<m2::PointD> points;
      points.reserve(region.Size() + 1);

      region.ForEachPoint([&points](PointT const & p)
      {
        points.push_back(
            PointUToPointD(m2::PointU(static_cast<uint32_t>(p.x), static_cast<uint32_t>(p.y)), kPointCoordBits));
      });

      fb.AddPolygon(std::move(points));
    };

    addPolygon(m_cell);
    for (auto const & region : m_regions)
      addPolygon(region);
  }
};
}  // namespace coastlines_generator

CoastlineFeaturesGenerator::CoastlineFeaturesGenerator() : m_merger(kPointCoordBits) {}

void CoastlineFeaturesGenerator::AddRegionToTree(feature::FeatureBuilder const & fb)
{
  ASSERT(fb.IsGeometryClosed(), ());

  fb.ForEachPolygon([&](auto const & polygon)
  {
    if (polygon.empty())
      return;

    using namespace coastlines_generator;
    RegionT rgn = CreateRegionI(polygon);
    auto const limitRect = GetLimitRect(rgn);

    m_tree.Add(std::move(rgn), limitRect);
  });
}

void CoastlineFeaturesGenerator::Process(feature::FeatureBuilder const & fb)
{
  if (fb.IsGeometryClosed())
    AddRegionToTree(fb);
  else
    m_merger(fb);
}

bool CoastlineFeaturesGenerator::Finish()
{
  coastlines_generator::DoAddToTree doAdd(*this);
  m_merger.DoMerge(doAdd);

  if (doAdd.HasNotMergedCoasts())
  {
    LOG(LINFO, ("Total not merged coasts:", doAdd.GetNotMergedCoastsCount()));
    LOG(LINFO, ("Total points in not merged coasts:", doAdd.GetNotMergedCoastsPoints()));
    return false;
  }

  return true;
}

class RegionInCellSplitter final
{
public:
  using TCell = RectId;
  using TIndex = m4::Tree<m2::RegionI>;
  using TProcessResultFunc = std::function<void(TCell const &, coastlines_generator::DoDifference &)>;
  using Regions = std::vector<coastlines_generator::RegionT>;

  static int constexpr kStartLevel = 4;
  static int constexpr kHighLevel = 10;
  static int constexpr kMaxPoints = 20000;

protected:
  struct Task
  {
    TCell cell;
    std::shared_ptr<Regions const> regions;
  };

  struct Context
  {
    std::mutex mutexTasks;
    std::list<Task> listTasks;
    std::condition_variable listCondVar;
    size_t inWork = 0;
    size_t processedCells = 0;
    size_t emittedCells = 0;
    TProcessResultFunc processResultFunc;
  };

  Context & m_ctx;
  TIndex const & m_index;

  RegionInCellSplitter(Context & ctx, TIndex const & index) : m_ctx(ctx), m_index(index) {}

public:
  static bool Process(size_t numThreads, TIndex const & index, TProcessResultFunc funcResult)
  {
    /// @todo Replace with base::ComputationalThreadPool

    Context ctx;

    ctx.listTasks.push_back({TCell::Root(), nullptr});

    ctx.processResultFunc = funcResult;

    numThreads = std::max(size_t{1}, numThreads);
    std::vector<std::thread> threads;
    threads.reserve(numThreads);
    for (size_t i = 0; i < numThreads; ++i)
      threads.emplace_back(RegionInCellSplitter(ctx, index));

    for (auto & thread : threads)
      thread.join();

    // return true if listTask has no error cells
    return ctx.listTasks.empty();
  }

  bool ProcessCell(Task const & task, std::shared_ptr<Regions const> & childRegions)
  {
    auto const & cell = task.cell;
    // get rect cell
    double minX, minY, maxX, maxY;
    CellIdConverter<mercator::Bounds, TCell>::GetCellBounds(cell, minX, minY, maxX, maxY);

    using namespace coastlines_generator;
    // create rect region
    PointT arr[] = {D2I(m2::PointD(minX, minY)), D2I(m2::PointD(minX, maxY)), D2I(m2::PointD(maxX, maxY)),
                    D2I(m2::PointD(maxX, minY))};
    RegionT rectR(arr, arr + ARRAY_SIZE(arr));

    // Do 'and' with all regions and accumulate the result, including bound region.
    // In 'odd' parts we will have an ocean.
    DoDifference doDiff(rectR);
    if (task.regions)
    {
      for (auto const & region : *task.regions)
        doDiff(region);
    }
    else
    {
      // Only the root reads whole coastlines. Descendants reuse clipped pieces
      // so a small coastal cell never needs to clip an entire continent again.
      m_index.ForEachInRect(GetLimitRect(rectR), std::ref(doDiff));
    }

    if (cell.Level() < kStartLevel || (cell.Level() < kHighLevel && doDiff.GetPointsCount() >= kMaxPoints))
    {
      // The cell rectangle is an output water mask, not a coastline ring.
      // Pass only coastline pieces to the four children, sharing them read-only.
      childRegions = std::make_shared<Regions>(doDiff.TakeRegions());
      return false;
    }

    m_ctx.processResultFunc(cell, doDiff);
    return true;
  }

  void operator()()
  {
    // thread main loop
    while (true)
    {
      std::unique_lock lock(m_ctx.mutexTasks);
      m_ctx.listCondVar.wait(lock, [&] { return (!m_ctx.listTasks.empty() || m_ctx.inWork == 0); });
      if (m_ctx.listTasks.empty())
        break;

      Task currentTask = std::move(m_ctx.listTasks.front());
      m_ctx.listTasks.pop_front();
      ++m_ctx.inWork;
      lock.unlock();

      std::shared_ptr<Regions const> childRegions;
      bool const done = ProcessCell(currentTask, childRegions);

      lock.lock();
      // return to queue not ready cells
      if (!done)
      {
        // Process descendants first to release large parent geometries sooner.
        for (int8_t i = 0; i < TCell::MAX_CHILDREN; ++i)
          m_ctx.listTasks.push_front({currentTask.cell.Child(i), childRegions});
      }
      else
      {
        ++m_ctx.emittedCells;
      }
      --m_ctx.inWork;
      ++m_ctx.processedCells;
      if (currentTask.cell.Level() <= 1 || m_ctx.processedCells % 1000 == 0 ||
          (m_ctx.listTasks.empty() && m_ctx.inWork == 0))
      {
        LOG(LINFO, ("Coastline cells processed:", m_ctx.processedCells, "emitted:", m_ctx.emittedCells,
                    "queued:", m_ctx.listTasks.size(), "active:", m_ctx.inWork));
      }
      m_ctx.listCondVar.notify_all();
      // Releasing a parent can free a large geometry buffer; do it outside the queue lock.
      lock.unlock();
    }
  }
};

std::vector<feature::FeatureBuilder> CoastlineFeaturesGenerator::GetFeatures(size_t maxThreads)
{
  uint32_t const coastType = ftypes::IsCoastlineChecker::Instance().GetType();

  std::vector<feature::FeatureBuilder> features;
  std::mutex featuresMutex;

  RegionInCellSplitter::Process(
      maxThreads, m_tree,
      [&](RegionInCellSplitter::TCell const & cell, coastlines_generator::DoDifference & cellData)
  {
    feature::FeatureBuilder fb;
    fb.SetCoastCell(cell.ToInt64(RegionInCellSplitter::kHighLevel + 1));

    cellData.AssignGeometry(fb);
    fb.SetArea();
    fb.AddType(coastType);

    // Should represent non-empty geometry
    CHECK_GREATER(fb.GetPolygonsCount(), 0, ());
    CHECK_GREATER_OR_EQUAL(fb.GetPointsCount(), 3, ());

    std::lock_guard lock(featuresMutex);
    features.emplace_back(std::move(fb));
  });

  return features;
}
