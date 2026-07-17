// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
#include "platform/country_file.hpp"

#include "base/assert.hpp"

#include "defines.hpp"

namespace platform
{
std::string GetFileName(std::string const & countryName, MapFileType type)
{
  ASSERT(!countryName.empty(), ());

  switch (type)
  {
  case MapFileType::Map: return countryName + DATA_FILE_EXTENSION;
  case MapFileType::Diff: return countryName + DIFF_FILE_EXTENSION;
  case MapFileType::Count: break;
  }

  UNREACHABLE();
}

CountryFile::CountryFile() : m_mapSize(0), m_hikingbookProMapSize(0) {}

CountryFile::CountryFile(std::string name) : m_name(std::move(name)), m_mapSize(0), m_hikingbookProMapSize(0) {}

CountryFile::CountryFile(std::string name, MwmSize size, std::string hash, MwmSize hikingbookProMapSize, std::string hikingbookProMapHash)
  : m_name(std::move(name))
  , m_mapSize(size)
  , m_hash(std::move(hash))
  , m_hikingbookProMapSize(hikingbookProMapSize)
  , m_hikingbookProMapHash(std::move(hikingbookProMapHash))
{}

std::string DebugPrint(CountryFile const & file)
{
  return "CountryFile [" + file.m_name + "]";
}
}  // namespace platform
