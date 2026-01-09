// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
#include "storage/map_files_downloader_with_ping.hpp"

#include "storage/pinger.hpp"

#include "base/assert.hpp"

namespace storage
{
std::map<MapSource, downloader::MetaConfig> MapFilesDownloaderWithPing::GetMetaConfig()
{
  std::map<MapSource, downloader::MetaConfig> metaConfigMap = LoadMetaConfigMap();
  for (auto & [_, metaConfig] : metaConfigMap) {
    CHECK(!metaConfig.servers.empty(), ());

    // Sort the list of servers by latency.
    auto const sorted = Pinger::ExcludeUnavailableAndSortEndpoints(metaConfig.servers);
    // Keep the original list if all servers are unavailable.
    if (!sorted.empty())
      metaConfig.servers = sorted;
  }
  
  return metaConfigMap;
}
}  // namespace storage
