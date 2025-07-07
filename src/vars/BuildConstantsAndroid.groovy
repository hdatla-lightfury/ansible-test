package vars
class BuildConstantsAndroid {
    static BUILD_CONFIG_VALUES = ['ALL', 'Test', 'Shipping', 'Debug', 'Development']
    static BUILD_CONFIG_VALUES_NIGHTLY = ['ALL', 'Test', 'Shipping']
    static BUILD_TARGET_VALUES = ['ALL', 'Titan', 'TitanGDC']
    static BUILD_TARGET_VALUES_NIGHTLY = ['ALL', 'Titan']
    static STREAMS =   [
                        "ALL",
                        "//titan-game/development",
                         "//titan-game/mainline",
                         "//titan-game/dev-engine"
                    ]
    static STREAMS_NIGHTLY =  [
                                "ALL",
                                "//titan-game/development",
                            ]
}