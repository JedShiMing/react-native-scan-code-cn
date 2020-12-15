# react-native-scan-code-cn

## Getting started
### 1. Installation
Using Npm

`npm install react-native-scan-code-cn --save`

Using Yarn

`yarn add react-native-scan-code-cn`

Linking (for React Native < 0.60 only)

`react-native link react-native-scan-code-cn`


### 2. Configuration
- android 
1. run `npx jetify`
2. In `android/app/src/main/AndroidManifest.xml` add:
```
<uses-permission android:name="android.permission.CAMERA" />
```

- iOS 
1. run `npx pod-install`
2. On iOS, you must update Info.plist with a usage description for camera
```
<key>NSCameraUsageDescription</key>
<string>Your own description of the purpose</string>
```

## Property
Prop | Type | Default | Note
---|---|---|---
codeTypes | Array<string> | all | 设置扫码类型
onBarCodeRead | callback() | | 扫码结果回调
onLightBright | callback() | | 当前后置摄像头光源回调
setFlashlight | function | | 设置手电筒开关


## Examples
Make sure permissions are turned on before using
```js
import React, { useState, useEffect } from 'react'
import { View, Platform, Animated, InteractionManager, Easing, TouchableOpacity, Text, StyleSheet, Dimensions } from 'react-native'
import { RNScanCode } from 'react-native-scan-code-cn'

const windowWidth = Dimensions.get('window').width

const AMBIENT_BRIGHTNESS_DARK = Platform.OS === 'ios' ? 0 : 60

// 是否打开闪光灯
let onFlash = false
let animation = new Animated.Value(0)

const ScanQrcodeScreen = ({ navigation, onclose }) => {
    // 是否显示手电筒按钮
    const [isFlashlight, setIsFlashlight] = useState(false)
    // 显示手电筒的什么按钮
    const [flashlightType, setFlashlightType] = useState('open')

    useEffect(() => {
        InteractionManager.runAfterInteractions(() => {
            Animated.loop(
                Animated.timing(animation, {
                    toValue: 275,
                    duration: 1500,
                    easing: Easing.linear,
                    useNativeDriver: true
                })
            ).start()
        })
    }, [])

    function barcodeReceived(e) {
        console.log('扫码结果 = ', e);
    }

    // 手电筒逻辑
    function FlashView() {
        return flashlightType === 'open' ? (
            <TouchableOpacity
                style={{ width: 100, height: 100, backgroundColor: 'green' }}
                onPress={() => {
                    RNScanCode.setFlashlight(true)
                    onFlash = true
                    setFlashlightType('close')
                }}
            >
                <Text>open</Text>
            </TouchableOpacity>
        ) : (
                <TouchableOpacity
                    style={{ width: 100, height: 100, backgroundColor: 'yellow' }}
                    onPress={() => {
                        RNScanCode.setFlashlight(false)
                        onFlash = false
                        setFlashlightType('open')
                    }}
                >
                    <Text>close</Text>
                </TouchableOpacity>
            )
    }

    return (
        <View style={{ flex: 1 }}>
            <RNScanCode
                style={style.preview}
                codeTypes={[RNScanCode.Constants.CodeType.qr]}
                onBarCodeRead={barcodeReceived}
                onLightBright={(data: any) => {
                    console.log('light = ', data.light)
                    if (!onFlash) {
                        if (Number(data.light) < AMBIENT_BRIGHTNESS_DARK) {
                            setIsFlashlight(true)
                        } else {
                            setIsFlashlight(false)
                        }
                    }
                }}
            >
                <View style={style.scanView} pointerEvents="none">
                    <View style={style.scanAnimateView}>
                        <Animated.View
                            style={{
                                transform: [
                                    {
                                        translateY: animation
                                    }
                                ],
                                backgroundColor: 'red',
                                width: 100,
                                height: 5,
                            }}
                        />
                    </View>
                </View>
                {isFlashlight && <FlashView />}
            </RNScanCode>
        </View>
    )
}

const style = StyleSheet.create({
    preview: {
        flex: 1,
        flexDirection: 'column',
        alignItems: 'center'
    },
    scanView: {
        width: windowWidth,
        height: 281,
        marginTop: 61,
        alignItems: 'center'
    },
    scanAnimateView: {
        width: windowWidth,
        height: 281,
        position: 'absolute',
        top: 0,
        left: 0,
        alignItems: 'center',
    },
})
```

https://github.com/JedShiMing/scan-code-demo

## Troubleshooting
#### 1. ios can't zoom
Add to the component
```
pointerEvents="none"
```