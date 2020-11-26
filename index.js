import React from 'react'
import {
    View,
    requireNativeComponent,
    NativeModules,
    Platform,
    Dimensions,
    StatusBar,
    DeviceEventEmitter
} from 'react-native'

// const ScanCodeManager = Platform.OS == 'ios' ? NativeModules.RCTScanCodeManager : NativeModules.RNScanCode

const NativeBarCode = requireNativeComponent(Platform.OS == 'ios' ? 'ScanCode' : 'RNScanCode', RNScanCode)

export default class RNScanCode extends React.Component {
    // static defaultProps = {
    //     barCodeTypes: Object.values(ScanCodeManager.barCodeTypes)
    // }

    // static propTypes = {
    //     ...View.propTypes,
    //     onBarCodeRead: PropTypes.func.isRequired,
    //     barCodeTypes: PropTypes.arrayOf(PropTypes.string)
    // }

    componentDidMount() {
        let that = this
        DeviceEventEmitter.addListener('RNScanCodeLightBright', function(e) {
            if (that.props.onLightBright) {
                that.props.onLightBright(e)
            }
        });
    }

    componentWillUnmount() {
        DeviceEventEmitter.removeListener('RNScanCodeLightBright')
    }
    
    render() {
        const { children, style, onLightBright, ...otherProps } = this.props
        console.log('onLightBright = ', onLightBright);
        const {width, height} =  Dimensions.get('window')
        let _height = height + (Platform.OS === 'ios' ? 0 : StatusBar.currentHeight)

        return (
            <View style={{flex: 1, backgroundColor: 'green'}}>
                <NativeBarCode
                    style={{width, height: _height}}
                    {...otherProps}
                />
                <View style={[{ position: 'absolute', top: 0, left: 0 }, style]}>
                    {children}
                </View>
            </View>
        )
    }
}


