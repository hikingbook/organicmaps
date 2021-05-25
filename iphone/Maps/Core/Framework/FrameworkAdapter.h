//
//  FrameworkAdapter.h
//  Hikingbook
//
//  Created by Zheng-Xiang Ke on 2016/4/9.
//  Copyright © 2016年 Zheng-Xiang Ke. All rights reserved.
//


@class MapViewController;

@interface FrameworkAdapter : NSObject

+ (FrameworkAdapter *)shared;

- (MapViewController *)getMapViewController:(UIApplication *)application;

@end
