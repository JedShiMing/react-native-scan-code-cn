import { Component } from 'react';
import { ViewProps } from 'react-native';

/** 支持的扫描码 */
type CodeType = Readonly<{
    aztec: any;
    code128: any;
    code39: any;
    code39mod43: any;
    code93: any;
    ean13: any;
    ean8: any;
    pdf417: any;
    qr: any;
    upc_e: any;
    interleaved2of5: any;
    itf14: any;
    datamatrix: any;
  }>;

export interface Constants {
    CodeType: CodeType;
}

/** 扫描回调接口参数 */
export interface BarCodeReadEvent {
    data: {
        type: keyof CodeType;
        code: string
    }
}

export interface RNScanCodeProps extends ViewProps {
    // 扫码回调方法
    onBarCodeRead?(event: BarCodeReadEvent): void;
    // 光源回调
    onLightBright?: (num: string) => void;
    // 扫码类型
    codeTypes?: Array<keyof CodeType>;
}


export class RNScanCode extends Component<RNScanCodeProps> {
    static Constants: Constants;
    // 设置手电筒
    static setFlashlight: (flash: boolean) => void
}