import Foundation
import ComposeApp

#if targetEnvironment(simulator)

final class IOSMlKitTranslationBridge: NSObject, IosNativeTranslationBridge {
    var isSupported: Bool { false }

    func downloadedLanguagePacks(callback: IosStringListCallback) {
        callback.complete(values: [], errorMessage: nil)
    }

    func downloadPair(
        sourceLanguage: String,
        targetLanguage: String,
        wifiOnly: Bool,
        callback: IosBooleanCallback
    ) {
        callback.complete(value: false, errorMessage: "Offline translation is not available in the iOS Simulator.")
    }

    func cancelPairDownload(sourceLanguage: String, targetLanguage: String) {}

    func deleteLanguagePack(languageCode: String, callback: IosBooleanCallback) {
        callback.complete(value: false, errorMessage: "Offline translation is not available in the iOS Simulator.")
    }

    func translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        callback: IosStringCallback
    ) {
        callback.complete(value: nil, errorMessage_: "Offline translation is not available in the iOS Simulator.")
    }
}

#else

import MLKitCommon
import MLKitTranslate

final class IOSMlKitTranslationBridge: NSObject, IosNativeTranslationBridge {
    private var translators: [String: Translator] = [:]
    private var activeDownloads: [String: IosBooleanCallback] = [:]

    var isSupported: Bool { true }

    func downloadedLanguagePacks(callback: IosStringListCallback) {
        let codes = ModelManager.modelManager().downloadedTranslateModels.map { model in
            model.language.rawValue
        }
        callback.complete(values: codes, errorMessage: nil)
    }

    func downloadPair(
        sourceLanguage: String,
        targetLanguage: String,
        wifiOnly: Bool,
        callback: IosBooleanCallback
    ) {
        guard let translator = translator(sourceCode: sourceLanguage, targetCode: targetLanguage) else {
            callback.complete(value: false, errorMessage: "Unsupported translation language pair.")
            return
        }
        let key = pairKey(sourceLanguage, targetLanguage)
        guard activeDownloads[key] == nil else {
            callback.complete(value: false, errorMessage: "A download for this pair is already running.")
            return
        }
        activeDownloads[key] = callback
        let conditions = ModelDownloadConditions(
            allowsCellularAccess: !wifiOnly,
            allowsBackgroundDownloading: true
        )
        translator.downloadModelIfNeeded(with: conditions) { [weak self] error in
            guard let self, let activeCallback = self.activeDownloads.removeValue(forKey: key) else {
                return
            }
            activeCallback.complete(value: error == nil, errorMessage: error?.localizedDescription)
        }
    }

    func cancelPairDownload(sourceLanguage: String, targetLanguage: String) {
        let key = pairKey(sourceLanguage, targetLanguage)
        activeDownloads.removeValue(forKey: key)?.complete(
            value: false,
            errorMessage: "Translation download canceled."
        )
    }

    func deleteLanguagePack(languageCode: String, callback: IosBooleanCallback) {
        guard let language = language(for: languageCode) else {
            callback.complete(value: false, errorMessage: "Unsupported language pack.")
            return
        }
        translators.removeAll()
        let model = TranslateRemoteModel.translateRemoteModel(language: language)
        ModelManager.modelManager().deleteDownloadedModel(model) { error in
            callback.complete(value: error == nil, errorMessage: error?.localizedDescription)
        }
    }

    func translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        callback: IosStringCallback
    ) {
        guard let translator = translator(sourceCode: sourceLanguage, targetCode: targetLanguage) else {
            callback.complete(value: nil, errorMessage_: "Unsupported translation language pair.")
            return
        }
        translator.translate(text) { translatedText, error in
            callback.complete(value: translatedText, errorMessage_: error?.localizedDescription)
        }
    }

    private func translator(sourceCode: String, targetCode: String) -> Translator? {
        guard let source = language(for: sourceCode), let target = language(for: targetCode), source != target else {
            return nil
        }
        let key = pairKey(sourceCode, targetCode)
        if let cached = translators[key] { return cached }
        let value = Translator.translator(
            options: TranslatorOptions(sourceLanguage: source, targetLanguage: target)
        )
        translators[key] = value
        return value
    }

    private func pairKey(_ source: String, _ target: String) -> String {
        "\(source)->\(target)"
    }

    private func language(for code: String) -> TranslateLanguage? {
        switch code.lowercased() {
        case "en": return .english
        case "es": return .spanish
        case "fr": return .french
        case "de": return .german
        case "ja": return .japanese
        case "fa": return .persian
        case "it": return .italian
        case "pt": return .portuguese
        default: return nil
        }
    }
}

#endif
