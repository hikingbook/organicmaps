// This file is updated for Hikingbook Pro Maps by Zheng-Xiang Ke on 2022.
#import <CoreApi/MWMStorage.h>

NS_ASSUME_NONNULL_BEGIN

@interface MWMStorage (UI)

- (void)downloadNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource;
- (void)downloadNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource onSuccess:(nullable MWMVoidBlock)success;
- (void)updateNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource;
- (void)updateNode:(NSString *)countryId mapSource:(MWMMapSource)mapSource onCancel:(nullable MWMVoidBlock)cancel;
- (void)deleteNode:(NSString *)countryId;
- (void)downloadNodes:(NSArray<NSString *> *)countryIds mapSources:(NSArray<NSNumber *> *)mapSources onSuccess:(nullable MWMVoidBlock)success;

- (NSDictionary<NSString *, id> *)downloadNodeWithResult:(NSString *)countryId
                                                           mapSource:(MWMMapSource)mapSource
                                                           NS_SWIFT_NAME(downloadNodeWithResult(countryId:mapSource:));
- (NSDictionary<NSString *, id> *)downloadNodesWithResult:(NSArray<NSString *> *)countryIds
                                                           mapSources:(NSArray<NSNumber *> *)mapSources
                                                           NS_SWIFT_NAME(downloadNodesWithResult(countryIds:mapSources:));


@end

NS_ASSUME_NONNULL_END
