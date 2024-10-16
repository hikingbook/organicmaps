#import "MWMMapDownloaderCellHeader.h"
#import "Hikingbook-Swift-Header.h"

@implementation MWMMapDownloaderCellHeader

- (instancetype)initWithFrame:(CGRect)frame
{
  self = [super initWithFrame:frame];
  if (self)
  {
    [self setStyleAndApply:@"regular12:blackSecondaryText"];
  }
  return self;
}

- (void)drawTextInRect:(CGRect)rect
{
  rect = UIEdgeInsetsInsetRect(rect, UIEdgeInsetsMake(0, 16, 0, 0));
  rect = UIEdgeInsetsInsetRect(rect, self.safeAreaInsets);
  [super drawTextInRect:rect];
}

@end
