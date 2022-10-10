// This file is updated for Hikingbook Topo Maps by Zheng-Xiang Ke on 2022.
#import "MWMStorage+UI.h"
#import "MWMAlertViewController.h"

@implementation MWMStorage (UI)

- (void)handleError:(NSError *)error {
  if (error.code == kStorageNotEnoughSpace) {
    [[MWMAlertViewController activeAlertController] presentNotEnoughSpaceAlert];
  } else if (error.code == kStorageNoConnection) {
    [[MWMAlertViewController activeAlertController] presentNoConnectionAlert];
  } else if (error.code == kStorageRoutingActive) {
    [[MWMAlertViewController activeAlertController] presentDeleteMapProhibitedAlert];
  } else {
    NSAssert(NO, @"Unknown error code");
  }
}

- (void)downloadNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource {
  [self downloadNode:countryId mapSource:mapSource onSuccess:nil];
}

- (void)downloadNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource onSuccess:(MWMVoidBlock)success {
  NSError *error;
  [self downloadNode:countryId mapSource:mapSource error:&error];
  if (error) {
    if (error.code == kStorageCellularForbidden) {
      __weak __typeof(self) ws = self;
      [[MWMAlertViewController activeAlertController] presentNoWiFiAlertWithOkBlock:^{
        [self enableCellularDownload:YES];
        [ws downloadNode:countryId mapSource:mapSource];
      } andCancelBlock:nil];
    } else {
      [self handleError:error];
    }
    return;
  }
  if (success) {
    success();
  }
}

- (void)updateNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource {
  [self updateNode:countryId mapSource:mapSource onCancel:nil];
}

- (void)updateNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource onCancel:(MWMVoidBlock)cancel {
  NSError *error;
  [self updateNode:countryId mapSource:mapSource error:&error];
  if (error) {
    if (error.code == kStorageCellularForbidden) {
      __weak __typeof(self) ws = self;
      [[MWMAlertViewController activeAlertController] presentNoWiFiAlertWithOkBlock:^{
        [self enableCellularDownload:YES];
        [ws updateNode:countryId mapSource:mapSource onCancel:cancel];
      } andCancelBlock:cancel];
    } else {
      [self handleError:error];
      if (cancel) {
        cancel();
      }
    }
  }
}

- (void)deleteNode:(NSString *)countryId {
  [self deleteNode:countryId ignoreUnsavedEdits:NO];
}

- (void)deleteNode:(NSString *)countryId ignoreUnsavedEdits:(BOOL)force {
  NSError *error;
  [self deleteNode:countryId ignoreUnsavedEdits:force error:&error];
  if (error) {
    __weak __typeof(self) ws = self;
    if (error.code == kStorageCellularForbidden) {
      [[MWMAlertViewController activeAlertController] presentNoWiFiAlertWithOkBlock:^{
        [self enableCellularDownload:YES];
        [ws deleteNode:countryId];
      } andCancelBlock:nil];
    } else if (error.code == kStorageHaveUnsavedEdits) {
      [[MWMAlertViewController activeAlertController] presentUnsavedEditsAlertWithOkBlock:^ {
        [ws deleteNode:countryId ignoreUnsavedEdits:YES];
      }];
    } else {
      [self handleError:error];
    }
  }
}

- (void)downloadNodes:(NSArray<NSString *> *)countryIds mapSources:(NSArray<NSNumber *> *)mapSources onSuccess:(nullable MWMVoidBlock)success {
  NSError *error;
    [self downloadNodes:countryIds mapSources:mapSources error:&error];
  if (error) {
    if (error.code == kStorageCellularForbidden) {
      __weak __typeof(self) ws = self;
      [[MWMAlertViewController activeAlertController] presentNoWiFiAlertWithOkBlock:^{
        [self enableCellularDownload:YES];
          [ws downloadNodes:countryIds mapSources:mapSources onSuccess:success];
      } andCancelBlock:nil];
    } else {
      [self handleError:error];
    }
    return;
  }
  if (success) {
    success();
  }
}

@end
