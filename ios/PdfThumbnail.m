#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(PdfThumbnail, NSObject)

RCT_EXTERN_METHOD(generate:(NSString *)filePath withPage:(int)page
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(generateWithMinSide:(NSString *)filePath withPage:(int)page withMinSide:(int)minSide
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(generateAllPages:(NSString *)filePath
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

@end
