//
//  FrameworkAdapter.mm
//  Hikingbook
//
//  Created by Zheng-Xiang Ke on 2016/4/9.
//  Copyright © 2016年 Zheng-Xiang Ke. All rights reserved.
//

#import "FrameworkAdapter.h"
#import "MapViewController.h"
#import "MapsAppDelegate.h"

@implementation FrameworkAdapter

+ (FrameworkAdapter *)shared
{
    static FrameworkAdapter *frameworkAdapter = nil;
    
    static dispatch_once_t onceSecurePredicate;
    dispatch_once(&onceSecurePredicate,^{
        frameworkAdapter = [[self alloc] init];
    });
    
    return frameworkAdapter;
}

- (MapViewController *)getMapViewController:(UIApplication *)application {
    return [MapsAppDelegate theApp].mapViewController;
}

@end
