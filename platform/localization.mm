#include "platform/localization.hpp"

#include <algorithm>

#import <Foundation/Foundation.h>

namespace platform
{
std::string GetLocalizedTypeName(std::string const & type)
{
  auto key = "type." + type;
  std::replace(key.begin(), key.end(), '-', '.');
  std::replace(key.begin(), key.end(), ':', '_');

  return [NSLocalizedStringFromTable(@(key.c_str()), @"OrganicMaps-Localizable", @"") UTF8String];
}

std::string GetLocalizedBrandName(std::string const & brand)
{
  auto const key = "brand." + brand;
  return [NSLocalizedStringFromTable(@(key.c_str()), @"OrganicMaps-Localizable", @"") UTF8String];
}

std::string GetLocalizedString(std::string const & key)
{
  return [NSLocalizedStringFromTable(@(key.c_str()), @"OrganicMaps-Localizable", @"") UTF8String];
}

std::string GetCurrencySymbol(std::string const & currencyCode)
{
  NSLocale * locale = [NSLocale currentLocale];
  NSString * symbol = [locale displayNameForKey:NSLocaleCurrencySymbol
                                          value:@(currencyCode.c_str())];
  if (!symbol)
    return currencyCode;

  return [symbol UTF8String];
}

std::string GetLocalizedMyPositionBookmarkName()
{
  NSDateFormatter * dateFormatter = [[NSDateFormatter alloc] init];
  dateFormatter.dateStyle = NSDateFormatterLongStyle;
  dateFormatter.timeStyle = NSDateFormatterShortStyle;
  NSDate * now = [NSDate date];
  return [dateFormatter stringFromDate:now].UTF8String;
}
}  // namespace platform
