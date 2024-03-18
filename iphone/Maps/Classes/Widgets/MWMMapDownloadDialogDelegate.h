//
//  MWMMapDownloadDialogDelegate.h
//  Hikingbook
//
//  Created by Kf on 2021/5/10.
//  Copyright © 2021 Zheng-Xiang Ke. All rights reserved.
//

#import <CoreApi/MWMStorage.h>

@class MWMMapDownloadDialog;

@protocol MWMMapDownloadDialogDelegate <NSObject>

@optional
-(BOOL)downloadDialog:(MWMMapDownloadDialog *)downloadDialog shouldDownloadMap:(NSString *)countryId;
-(void)downloadDialog:(MWMMapDownloadDialog *)downloadDialog updateNumDownloadedMapsLimitLabel:(UILabel *)numDownloadedMapsLimitLabel isDownloading:(BOOL)isDownloading isInQueue:(BOOL)isInQueue;
-(MWMMapSource)downloadDialog:(MWMMapDownloadDialog *)downloadDialog mapSourceForCountry:(NSString *)countryId;
-(void)downloadDialog:(MWMMapDownloadDialog *)downloadDialog presentMapStyle:(NSString *)countryId;
-(NSString *)downloadDialog:(MWMMapDownloadDialog *)downloadDialog l10nMapSource:(MWMMapSource)mapSorce;
-(void)downloadDialog:(MWMMapDownloadDialog *)downloadDialog cancelDownload:(NSString *)countryId mapSource:(MWMMapSource)mapSorce;

@end
