// This file is updated for Hikingbook Topo Maps by Zheng-Xiang Ke on 2022.
#pragma once

#include <cstdint>
#include <string>
#include <utility>

// Note: new values must be added before MapFileType::Count.
enum class MapFileType : uint8_t
{
  Map,
  Diff,

  Count
};

using MwmCounter = uint32_t;
using MwmSize = uint64_t;
using LocalAndRemoteSize = std::pair<MwmSize, MwmSize>;

std::string DebugPrint(MapFileType type);

// Hikingbook Topo Maps
enum class MapSource : uint8_t
{
  Organicmaps,
  HikingbookProMaps,

  Count
};
