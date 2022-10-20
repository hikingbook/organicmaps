// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
#include "storage/queued_country.hpp"

#include "storage/storage_helpers.hpp"

#include "platform/downloader_utils.hpp"
#include "platform/local_country_file_utils.hpp"

#include "base/assert.hpp"

namespace storage
{
QueuedCountry::QueuedCountry(platform::CountryFile const & countryFile, CountryId const & countryId,
                             MapFileType type, int64_t currentDataVersion,
                             std::string const & dataDir,
                             diffs::DiffsSourcePtr const & diffs,
                             MapSource const mapSource)
  : m_countryFile(countryFile)
  , m_countryId(countryId)
  , m_fileType(type)
  , m_currentDataVersion(currentDataVersion)
  , m_dataDir(dataDir)
  , m_diffsDataSource(diffs)
  , m_mapSource(mapSource)
{
  ASSERT(IsCountryIdValid(GetCountryId()), ("Only valid countries may be downloaded."));
  ASSERT(m_diffsDataSource != nullptr, ());
}

void QueuedCountry::Subscribe(Subscriber & subscriber)
{
  m_subscriber = &subscriber;
}

void QueuedCountry::Unsubscribe()
{
  m_subscriber = nullptr;
}

void QueuedCountry::SetFileType(MapFileType type)
{
  m_fileType = type;
}

MapFileType QueuedCountry::GetFileType() const
{
  return m_fileType;
}

CountryId const & QueuedCountry::GetCountryId() const
{
  return m_countryId;
}

std::string QueuedCountry::GetRelativeUrl() const
{
  auto const fileName = m_countryFile.GetFileName(m_fileType);

  uint64_t diffVersion = 0;
  if (m_fileType == MapFileType::Diff)
    CHECK(m_diffsDataSource->VersionFor(m_countryId, diffVersion), ());

  return downloader::GetFileDownloadUrl(fileName, m_currentDataVersion, diffVersion);
}

std::string QueuedCountry::GetFileDownloadPath() const
{
  return platform::GetFileDownloadPath(m_currentDataVersion, m_dataDir, m_countryFile, m_fileType);
}

uint64_t QueuedCountry::GetDownloadSize() const
{
  uint64_t size;
  if (m_fileType == MapFileType::Diff)
  {
    CHECK(m_diffsDataSource->SizeToDownloadFor(m_countryId, size), ());
    return size;
  }

  return GetRemoteSize(*m_diffsDataSource, m_countryFile, m_mapSource);
}

bool QueuedCountry::isMapAvaliable() const
{
    switch (m_mapSource) {
        case MapSource::Organicmaps:
            return m_countryFile.IsOrganicMapAvailable();
        case MapSource::HikingbookProMaps:
            return m_countryFile.IsHikingbookProMapAvailable();
        default:
            return false;
    }
}

void QueuedCountry::OnCountryInQueue() const
{
  if (m_subscriber != nullptr)
    m_subscriber->OnCountryInQueue(*this);
}

void QueuedCountry::OnStartDownloading() const
{
  if (m_subscriber != nullptr)
    m_subscriber->OnStartDownloading(*this);
}

void QueuedCountry::OnDownloadProgress(downloader::Progress const & progress) const
{
  if (m_subscriber != nullptr)
    m_subscriber->OnDownloadProgress(*this, progress);
}

void QueuedCountry::OnDownloadFinished(downloader::DownloadStatus status) const
{
  if (m_subscriber != nullptr)
    m_subscriber->OnDownloadFinished(*this, status);
}

bool QueuedCountry::operator==(CountryId const & countryId) const
{
  return m_countryId == countryId;
}
}  // namespace storage
