// This file is modified by Zheng-Xiang Ke on 2021.
#import "MWMViewController.h"

#include "storage/storage_defines.hpp"

@class MapViewController;
@protocol MWMMapDownloadDialogDelegate;

@interface MWMMapDownloadDialog : UIView

+ (instancetype)dialogForController:(MapViewController *)controller;

- (void)processViewportCountryEvent:(storage::CountryId const &)countryId;
- (void)processCountryEvent:(NSString *)countryId;
- (NSString *)countryID;

@property(weak, nonatomic, nullable) id<MWMMapDownloadDialogDelegate> delegate;

@property(strong, nonatomic) IBOutlet UILabel *noteLabel;
@property(strong, nonatomic) IBOutlet UIButton *downloadButton;
@property(strong, nonatomic) IBOutlet UIView *progressWrapper;

@end
