// This file is modified by Zheng-Xiang Ke on 2018.
extension UIWindow {
  private func findFirstResponder(view: UIView) -> UIResponder? {
    guard !view.isFirstResponder else { return view }
    for subView in view.subviews {
      if let responder = findFirstResponder(view: subView) {
        return responder
      }
    }
    return nil
  }

//  @objc func firstResponder() -> UIResponder? {
//    return findFirstResponder(view: self)
//  }
}
