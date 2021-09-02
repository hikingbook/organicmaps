//
//  MWMMapDownloadDialogDelegate.h
//  Hikingbook
//
//  Created by Kf on 2021/5/10.
//  Copyright © 2021 Zheng-Xiang Ke. All rights reserved.
//

@class MWMMapDownloadDialog;

@protocol MWMMapDownloadDialogDelegate <NSObject>

@optional
-(BOOL)downloadDialog:(MWMMapDownloadDialog *)downloadDialog shouldDownloadMap:(NSString *)countryId;
-(void)downloadDialog:(MWMMapDownloadDialog *)downloadDialog updateNumDownloadedMapsLimitLabel:(UILabel *)numDownloadedMapsLimitLabel isDownloading:(BOOL)isDownloading isInQueue:(BOOL)isInQueue;

@end
