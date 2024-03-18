// This file is modified by Zheng-Xiang Ke on 2021.
#import "MWMViewController.h"

#include "storage/storage_defines.hpp"

@class MapViewController;
@protocol MWMMapDownloadDialogDelegate;

@interface MWMMapDownloadDialog : UIView

+ (instancetype)dialogForController:(MapViewController *)controller;

- (void)processViewportCountryEvent:(storage::CountryId const &)countryId;
- (void)processCountryEvent:(NSString *)countryId;

@property(weak, nonatomic, nullable) id<MWMMapDownloadDialogDelegate> delegate;

@end
