#include "routing/maxspeeds_serialization.hpp"

#include <utility>

namespace routing
{

MaxspeedsSerializer::FeatureSpeedMacro::FeatureSpeedMacro(uint32_t featureID, Maxspeed const & speed,
                                                          MaxspeedConverter const & converter)
  : m_featureID(featureID)
  , m_forward(converter.SpeedToMacro({speed.GetForward(), speed.GetUnits()}))
  , m_backward(converter.SpeedToMacro({speed.GetBackward(), speed.GetUnits()}))
{
  // We store bidirectional speeds only if they are not equal, see Maxspeed::IsBidirectional() comments.
  if (m_forward == m_backward)
    m_backward = SpeedMacro::Undefined;

  if (speed.HasConditional())
  {
    auto const conditional = converter.SpeedToMacro({speed.GetConditional(), speed.GetUnits()});
    if (conditional == SpeedMacro::Undefined)
      return;

    // A valid schedule may still be expired or unsupported by the routing encoding.
    // Only advertise a conditional speed when its complete payload can be written.
    std::vector<uint8_t> buffer;
    {
      MemWriter writer(buffer);
      BitWriter bitWriter(writer);
      auto serDes = GetSerDes();
      if (!serDes.Serialize(bitWriter, speed.GetConditionalTime()))
      {
        LOG(LWARNING, ("Skipping unserializable maxspeed condition for feature", featureID));
        return;
      }
    }
    m_conditional = conditional;
    m_conditionalTimeData = std::move(buffer);
  }
}

std::vector<uint8_t> MaxspeedsSerializer::GetForwardMaxspeeds(std::vector<FeatureSpeedMacro> const & speeds,
                                                              uint32_t & maxFeatureID)
{
  std::vector<uint8_t> result;
  for (auto const & s : speeds)
  {
    if (!s.IsComplex())
    {
      result.push_back(static_cast<uint8_t>(s.m_forward));
      maxFeatureID = s.m_featureID;
    }
  }
  return result;
}
}  // namespace routing
