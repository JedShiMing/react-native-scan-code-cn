#import "ScanCode.h"

#define MaxSCale 5.0  //最大缩放比例
#define MinScale 1.0  //最小缩放比例

@interface ScanCode ()
/** 捏合手势 */
@property (nonatomic, strong) UIPinchGestureRecognizer *pinchGestureRecognizer;
@end

@implementation ScanCode

#pragma mark - 初始化
- (id)initWithBridge:(RCTBridge *)bridge{
    if (self = [super init]) {
        self.sessionQueue = dispatch_queue_create("cameraQueue", DISPATCH_QUEUE_SERIAL);
        [self initQrCodeScanning];
    }
    return self;
}

#pragma mark - 视图移除时，释放资源
- (void)removeFromSuperview{
    [super removeFromSuperview];
    [self stopSession];
}

/** 创建取景器视图 */
- (void)layoutSubviews{
    [super layoutSubviews];
    self.previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    self.previewLayer.needsDisplayOnBoundsChange = YES;
    self.previewLayer.frame = self.bounds;
    [self setBackgroundColor:[UIColor blackColor]];
    [self.layer insertSublayer:self.previewLayer atIndex:0];
    self.pinchGestureRecognizer = [self createUIPinchGestureRecognizer];
    [self addGestureRecognizer:self.pinchGestureRecognizer];
}

/** 创建捏合手势 */
- (UIPinchGestureRecognizer*)createUIPinchGestureRecognizer{
    return [[UIPinchGestureRecognizer alloc] initWithTarget:self action:@selector(handlePinchToZoomRecognizer:)];
}

/** 捏合手势方法 */
- (void)handlePinchToZoomRecognizer:(UIPinchGestureRecognizer *)pinchRecognizer{
    // 控制捏合速度
    const CGFloat pinchVelocityDividerFactor = 10.0f;

    if (pinchRecognizer.state == UIGestureRecognizerStateChanged) {
        AVCaptureDevice *device = [self.input device];
        if(device == nil){
            return;
        }
        NSError *error = nil;
        float maxZoom = [self getMaxZoomFactor:device];
        if ([device lockForConfiguration:&error]) {
            CGFloat desiredZoomFactor = device.videoZoomFactor + atan2f(pinchRecognizer.velocity, pinchVelocityDividerFactor);
            // Check if desiredZoomFactor fits required range from 1.0 to activeFormat.videoMaxZoomFactor
            device.videoZoomFactor = MAX(MinScale, MIN(desiredZoomFactor, maxZoom));
            [device unlockForConfiguration];
        } else {
            NSLog(@"error: %@", error);
        }
    }
}

/** 设置捏合最大比例 */
- (float)getMaxZoomFactor:(AVCaptureDevice*)device {
    return MIN(MaxSCale, device.activeFormat.videoMaxZoomFactor);
}

#pragma mark - 初始化扫码
/**
 *  扫描二维码 大概的流程应该是：
 *  1.打开设备的摄像头
 *  2.进行二维码图像捕获
 *  3.获取捕获的图像进行解析
 *  4.取得解析结果进行后续的处理
 *  这些流程需要用到AVFoundation这个库，要完成一次扫描的过程，需要用到AVCaptureSession这个类
 *  这个session类把一次扫描看做一次会话，会话开始后才是正在的'扫描'开始
 */
- (void)initQrCodeScanning {
    // 代表不在模拟器上使用
#if !(TARGET_IPHONE_SIMULATOR)
    self.session = [[AVCaptureSession alloc] init];
    // 采集高质量
    [self.session setSessionPreset:AVCaptureSessionPresetHigh];
    
    // 设置相机取景器，要不然会黑屏
    self.previewLayer =
    [AVCaptureVideoPreviewLayer layerWithSession:self.session];
    
    // 获取摄像头设备
    self.device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    if (self.device == nil) {
        return;
    }
    
#endif
    return;
}

/** 打开、关闭手电筒 */
- (void)setFlashlight:(BOOL)isOpen{
    if (isOpen) {
        NSError *error = nil;
        if ([self.device hasTorch]) {
            BOOL locked = [self.device lockForConfiguration:&error];
            if (locked) {
                self.device.torchMode = AVCaptureTorchModeOn;
                [self.device unlockForConfiguration];
            }
        }
    } else {
        if ([self.device hasTorch]) {
            [self.device lockForConfiguration:nil];
            [self.device setTorchMode:AVCaptureTorchModeOff];
            [self.device unlockForConfiguration];
        }
    }
}

#pragma mark - 开启扫码
- (void)startSession{
#if !(TARGET_IPHONE_SIMULATOR)
    dispatch_async(self.sessionQueue, ^{
        if (self.metadataOutput == nil) {
            NSError *error = nil;
            //创建输入流
            self.input = [[AVCaptureDeviceInput alloc] initWithDevice:self.device error:&error];
            if ([self.session canAddInput:self.input]) {
                [self.session addInput:self.input];
            }
            // 拍完照片以后，需要一个AVCaptureMetadataOutput对象将获取的'图像'输出，以便进行对其解析
            AVCaptureMetadataOutput *output = [[AVCaptureMetadataOutput alloc] init];
            self.metadataOutput = output;
            // 创建视频输出流
            AVCaptureVideoDataOutput *videoOutput = [[AVCaptureVideoDataOutput alloc] init];
            self.videodataOutput = videoOutput;
            if ([self.session canAddOutput:self.metadataOutput]) {
                [self.metadataOutput setMetadataObjectsDelegate:self queue:self.sessionQueue];
                [self.session addOutput:self.metadataOutput];
                // 设置输出类型 有二维码 条形码等
                [self setScanCodeType];
                // 设置全屏扫描
                output.rectOfInterest = CGRectMake(0, 0, 1.0, 1.0);
            }
            if ([self.session canAddOutput:self.videodataOutput]) {
                [self.videodataOutput setSampleBufferDelegate:self queue:self.sessionQueue];
                [self.session addOutput:self.videodataOutput];
            }
        }
        [self.session startRunning];
    });
#endif
    return;
}

#pragma mark - 停止扫码
- (void)stopSession{
#if !(TARGET_IPHONE_SIMULATOR)
    dispatch_async(self.sessionQueue, ^{
        [self.previewLayer removeFromSuperlayer];
        [self.session commitConfiguration];
        [self.session stopRunning];
        for (AVCaptureInput *input in self.session.inputs) {
            [self.session removeInput:input];
        }
        for (AVCaptureOutput *output in self.session.outputs) {
            [self.session removeOutput:output];
        }
        self.metadataOutput = nil;
        self.videodataOutput = nil;
    });
#endif
}

/** 设置扫码类型 */
- (void)setScanCodeType{
    NSArray<AVMetadataObjectType> *codeTypes = [[NSArray alloc] init];
    NSArray<AVMetadataObjectType> *paramsCodeTypes = [NSArray arrayWithArray:self.codeTypes];
    NSArray<AVMetadataObjectType> *availableObjectTypes = self.metadataOutput.availableMetadataObjectTypes;
    
    for (AVMetadataObjectType objectType in paramsCodeTypes) {
        if ([availableObjectTypes containsObject:objectType]) {
            codeTypes = [codeTypes arrayByAddingObject:objectType];
        }
    }
    [self.metadataOutput setMetadataObjectTypes:codeTypes];
}

#pragma mark - AVCaptureMetadataOutputObjectsDelegate
/** 扫描回调方法 */
- (void)captureOutput:(AVCaptureOutput *)output didOutputMetadataObjects:(NSArray<__kindof AVMetadataObject *> *)metadataObjects fromConnection:(AVCaptureConnection *)connection{
    
    for (AVMetadataMachineReadableCodeObject *metadata in metadataObjects) {
        if (self.onBarCodeRead) {
            // 这就是扫描的结果
            self.onBarCodeRead(@{
                @"data": @{
                        @"type": metadata.type,
                        @"code": metadata.stringValue
                }});
            [self.session stopRunning];
        }
    }
}

#pragma mark- AVCaptureVideoDataOutputSampleBufferDelegate的方法
/** 光源感应 */
- (void)captureOutput:(AVCaptureOutput *)captureOutput didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    
    CFDictionaryRef metadataDict = CMCopyDictionaryOfAttachments(NULL,sampleBuffer, kCMAttachmentMode_ShouldPropagate);
    NSDictionary *metadata = [[NSMutableDictionary alloc] initWithDictionary:(__bridge NSDictionary*)metadataDict];
    CFRelease(metadataDict);
    NSDictionary *exifMetadata = [[metadata objectForKey:(NSString *)kCGImagePropertyExifDictionary] mutableCopy];
    float brightnessValue = [[exifMetadata objectForKey:(NSString *)kCGImagePropertyExifBrightnessValue] floatValue];
    
    if (self.onLightBright) {
        self.onLightBright(@{@"light": @(brightnessValue)});
    }
}

- (void)setCodeTypes:(NSArray *)codeTypes{
    _codeTypes = codeTypes;
    // 开始扫码
    [self startSession];
}

@end
