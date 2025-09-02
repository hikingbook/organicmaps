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

- (NSDictionary<NSString *, id> *)downloadNodeWithCountrID:(NSString *)countryId mapSource:(MWMMapSource)mapSource;
- (NSDictionary<NSString *, id> *)downloadNodesWithCountryIDs:(NSArray<NSString *> *)countryIds mapSources:(NSArray<NSNumber *> *)mapSources;
- (NSDictionary<NSString *, id> *)updateNodeWithCountryID:(NSString *)countryId mapSource:(MWMMapSource)mapSource;
- (NSDictionary<NSString *, id> *)updateNodesWithCountryIDs:(NSArray<NSString *> *)countryIds mapSources:(NSArray<NSNumber *> *)mapSources;

@end

NS_ASSUME_NONNULL_END
