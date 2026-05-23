// This file is modified by Zheng-Xiang Ke on 2018.
extension UIViewController {
  @objc static func topViewController() -> UIViewController {
    let window = UIApplication.shared.delegate!.window!!
    if let tabBarController = window.rootViewController as? UITabBarController, let viewControllers = tabBarController.viewControllers {
        for viewController in viewControllers {
            if let navigationController = viewController as? UINavigationController, let topViewController = navigationController.topViewController as? MWMViewController {
                return topViewController
            }
        }
        
        return tabBarController.selectedViewController!
    }
    else if let rootViewController = window.rootViewController as? UINavigationController {
        return rootViewController.topViewController!
    }
    else {
        return window.rootViewController!
    }
  }
}
