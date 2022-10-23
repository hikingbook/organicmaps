// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
#include "storage/map_files_downloader_with_ping.hpp"

#include "storage/pinger.hpp"

#include "platform/platform.hpp"

#include "base/assert.hpp"

namespace storage
{
void MapFilesDownloaderWithPing::GetMetaConfig(MetaConfigCallback const & callback)
{
  ASSERT(callback , ());

  std::map<MapSource, MetaConfig> metaConfigMap = LoadMetaConfigMap();
    for (auto & [_, metaConfig] : metaConfigMap) {
        CHECK(!metaConfig.m_serversList.empty(), ());

        // Sort the list of servers by latency.
        auto const sorted = Pinger::ExcludeUnavailableAndSortEndpoints(metaConfig.m_serversList);
        // Keep the original list if all servers are unavailable.
        if (!sorted.empty())
          metaConfig.m_serversList = sorted;
    }
  
  callback(metaConfigMap);
}
}  // namespace storage
