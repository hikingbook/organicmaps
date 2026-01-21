// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
#include "storage/map_files_downloader.hpp"

#include "storage/queued_country.hpp"

#include "platform/downloader_utils.hpp"
#include "platform/http_client.hpp"
#include "platform/locale.hpp"
#include "platform/platform.hpp"
#include "platform/products.hpp"
#include "platform/servers_list.hpp"
#include "platform/settings.hpp"

#include "coding/url.hpp"

#include "base/assert.hpp"

namespace storage
{
void MapFilesDownloader::DownloadMapFile(QueuedCountry && queuedCountry)
{
    if (!queuedCountry.isMapAvailable()) {
        return;
    }
    auto pos = m_serversList.find(queuedCountry.GetMapSource());
  if (pos != m_serversList.end() && !pos->second.empty())
  {
    Download(std::move(queuedCountry));
    return;
  }

  m_pendingRequests.Append(std::move(queuedCountry));

  if (!m_isMetaConfigRequested)
  {
    RunMetaConfigAsync([this]()
    {
      m_pendingRequests.ForEachCountry([this](QueuedCountry & country) { Download(std::move(country)); });

      m_pendingRequests.Clear();
    });
  }
}

void MapFilesDownloader::RunMetaConfigAsync(std::function<void()> && callback)
{
  m_isMetaConfigRequested = true;

  GetPlatform().RunTask(Platform::Thread::Network, [this, callback = std::move(callback)]()
  {
    auto metaConfigMap = GetMetaConfig();

    for (auto & [mapSource, metaConfig] : metaConfigMap) {
      // Thread-safe.
      settings::Update(metaConfig.settings);
      products::ProductsSettings::Instance().Update(std::move(metaConfig.productsConfig));
    }
    
    GetPlatform().RunTask(Platform::Thread::Gui, [this, metaConfigMap = metaConfigMap, callback = std::move(callback)]()
    {
        for (auto & [mapSource, metaConfig] : metaConfigMap) {
            m_serversList[mapSource] = std::move(metaConfig.servers);
        }

        callback();

        // Reset flag to invoke servers list downloading next time if current request has failed.
        m_isMetaConfigRequested = false;
    });
  });
}

void MapFilesDownloader::Remove(CountryId const & id)
{
  if (!m_pendingRequests.IsEmpty())
    m_pendingRequests.Remove(id);
}

void MapFilesDownloader::Clear()
{
  m_pendingRequests.Clear();
}

QueueInterface const & MapFilesDownloader::GetQueue() const
{
  return m_pendingRequests;
}

void MapFilesDownloader::DownloadAsString(std::string url, MapSource mapSource, std::function<bool(std::string const &)> && callback,
                                          bool forceReset /* = false */)
{
  EnsureMetaConfigReady(mapSource, [this, forceReset, mapSource, url = std::move(url), callback = std::move(callback)]()
  {
    auto pos = m_serversList.find(mapSource);
    if ((m_fileRequest && !forceReset) || pos == m_serversList.end() || pos->second.empty())
      return;

    // Servers are sorted from best to worst.
    m_fileRequest.reset(RequestT::Get(url::Join(pos->second.front(), url),
                                      [this, callback = std::move(callback)](RequestT & request)
    {
      bool deleteRequest = true;
      auto const & buffer = request.GetData();
      if (!buffer.empty())
      {
        // Update deleteRequest flag if new download was requested in callback.
        deleteRequest = !callback(buffer);
      }

      if (deleteRequest)
        m_fileRequest.reset();
    }));
  });
}

void MapFilesDownloader::EnsureMetaConfigReady(MapSource mapSource, std::function<void()> && callback)
{
    auto pos = m_serversList.find(mapSource);
  /// @todo Implement logic if m_metaConfig is "outdated".
  /// Fetch new servers list on each download request?
  if (pos != m_serversList.end() && !pos->second.empty())
  {
    callback();
  }
  else if (!m_isMetaConfigRequested)
  {
    RunMetaConfigAsync(std::move(callback));
  }
  else
  {
    // skip this request without callback call
  }
}

std::vector<std::string> MapFilesDownloader::MakeUrlListLegacy(MapSource mapSource, std::string const & fileName) const
{
  return MakeUrlList(mapSource, downloader::GetFileDownloadUrl(fileName, m_dataVersion));
}

void MapFilesDownloader::SetServersList(MapSource mapSource, ServersList const & serversList)
{
  m_serversList[mapSource] = serversList;
}

void MapFilesDownloader::SetDownloadingPolicy(DownloadingPolicy * policy)
{
  m_downloadingPolicy = policy;
}

bool MapFilesDownloader::IsDownloadingAllowed() const
{
  return m_downloadingPolicy == nullptr || m_downloadingPolicy->IsDownloadingAllowed();
}

std::vector<std::string> MapFilesDownloader::MakeUrlList(MapSource mapSource, std::string const & relativeUrl) const
{
  std::vector<std::string> urls;
    auto pos = m_serversList.find(mapSource);
    if (pos != m_serversList.end()) {
        auto serversList = pos->second;
        urls.reserve(serversList.size());
        for (auto const & server : serversList)
          urls.emplace_back(url::Join(server, relativeUrl));
    }

  return urls;
}

std::string GetAcceptLanguage()
{
  auto const locale = platform::GetCurrentLocale();
  return locale.m_language + "-" + locale.m_country;
}

// static
std::map<MapSource, downloader::MetaConfig> MapFilesDownloader::LoadMetaConfigMap()
{
    Platform & pl = GetPlatform();
    std::map<MapSource, std::string> metaServerUrls = { {MapSource::Organicmaps, pl.MetaServerUrl() }, { MapSource::HikingbookProMaps, pl.HikingbookProMapsMetaServerUrl() }};
    std::map<MapSource, downloader::MetaConfig> metaConfigMap;
    for (auto const & [mapSource, metaServerUrl] : metaServerUrls) {
        std::string httpResult;
        if (!metaServerUrl.empty())
        {
            platform::HttpClient request(metaServerUrl);
            request.SetRawHeader("X-OM-DataVersion", std::to_string(m_dataVersion));

            request.SetRawHeader("X-OM-AppVersion", pl.Version());
            request.SetRawHeader("Accept-Language", GetAcceptLanguage());
	
			/// @DebugNote Uncomment to check donates flow.
    		// request.SetRawHeader("X-OM-AppVersion", "2025.09.19-6-ios");  // "2025.09.15-18-FDroid"
    		// request.SetRawHeader("Accept-Language", "de-DE");

            request.SetTimeout(10.0);  // timeout in seconds
            request.RunHttpRequest(httpResult);
        }
        
        std::optional<downloader::MetaConfig> metaConfig = downloader::ParseMetaConfig(httpResult);
        if (!metaConfig)
        {
            metaConfig = downloader::ParseMetaConfig(pl.DefaultUrlsJSON(metaServerUrl));
            CHECK(metaConfig, ());
            LOG(LWARNING, ("Can't get meta configuration from request, using default servers:", metaConfig->servers));
        }
        metaConfigMap[mapSource] = *metaConfig;
    }
  return metaConfigMap;
}

std::map<MapSource, downloader::MetaConfig> MapFilesDownloader::GetMetaConfig()
{
  return LoadMetaConfigMap();
}

}  // namespace storage
