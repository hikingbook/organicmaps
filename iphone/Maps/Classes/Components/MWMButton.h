typedef NS_ENUM(NSUInteger, MWMButtonColoring)
{
  MWMButtonColoringOther,
  MWMButtonColoringBlue,
  MWMButtonColoringBlack,
  MWMButtonColoringWhite,
  MWMButtonColoringWhiteText,
  MWMButtonColoringGray,
  MWMButtonColoringPrimary // Added by Lei
};

@interface MWMButton : UIButton

@property (copy, nonatomic) NSString * imageName;
@property (nonatomic) MWMButtonColoring coloring;

@end
