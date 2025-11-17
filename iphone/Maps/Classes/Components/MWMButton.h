typedef NS_ENUM(NSUInteger, MWMButtonColoring) {
  MWMButtonColoringOther,
  MWMButtonColoringBlue,
  MWMButtonColoringBlack,
  MWMButtonColoringWhite,
  MWMButtonColoringWhiteText,
  MWMButtonColoringGray,
  MWMButtonColoringRed,
  MWMButtonColoringPrimary, // Added by Lei
  MWMButtonColoringBodySecondary // Added by Kf
};

@interface MWMButton : UIButton

@property(copy, nonatomic) NSString * imageName;
@property(nonatomic) MWMButtonColoring coloring;

@end
