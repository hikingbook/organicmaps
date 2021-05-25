//
//  MWMMapDownloadDialogDelegate.h
//  Hikingbook
//
//  Created by Kf on 2021/5/10.
//  Copyright © 2021 Zheng-Xiang Ke. All rights reserved.
//

#ifndef MWMMapDownloadDialogDelegate_h
#define MWMMapDownloadDialogDelegate_h

#include "storage/storage_defines.hpp"

@class MWMMapDownloadDialog;

@protocol MWMMapDownloadDialogDelegate <NSObject>

-(BOOL)downloadDialog:(MWMMapDownloadDialog *)downloadDialog shouldDownloadMap:(storage::CountryId const &)countryId;
-(void)downloadDialog:(MWMMapDownloadDialog *)downloadDialog updateNumDownloadedMapsLimitLabel:(UILabel *)numDownloadedMapsLimitLabel;

@end


#endif /* MWMMapDownloadDialogDelegate_h */
