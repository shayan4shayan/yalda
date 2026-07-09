import Foundation
import ComposeApp
import MLKitCommon
import MLKitTranslate

final class IOSMlKitTranslationBridge: NSObject, IosNativeTranslationBridge {
    private let modelId = "mlkit_en_fa"
    private let targetModel = TranslateRemoteModel.translateRemoteModel(language: .persian)
    private let translator = Translator.translator(
        options: TranslatorOptions(sourceLanguage: .english, targetLanguage: .persian)
    )
    private var activeDownloadProgress: Progress?
    private var activeDownloadCallback: IosBooleanCallback?
    private var activeDownloadModelId: String?
    private var successObserver: NSObjectProtocol?
    private var failureObserver: NSObjectProtocol?

    var isSupported: Bool {
        true
    }

    func isModelDownloaded(modelId: String, callback: IosBooleanCallback) {
        guard isKnownModel(modelId) else {
            callback.complete(value: false, errorMessage: nil)
            return
        }

        let isDownloaded = ModelManager.modelManager()
            .downloadedTranslateModels
            .contains { model in
                model.language == targetModel.language
            }
        callback.complete(value: isDownloaded, errorMessage: nil)
    }

    func downloadModel(modelId: String, callback: IosBooleanCallback) {
        guard isKnownModel(modelId) else {
            callback.complete(value: false, errorMessage: "Translation model was not found.")
            return
        }

        if ModelManager.modelManager().isModelDownloaded(targetModel) {
            callback.complete(value: true, errorMessage: nil)
            return
        }

        guard activeDownloadProgress == nil else {
            callback.complete(value: false, errorMessage: "A model download is already running.")
            return
        }

        let conditions = ModelDownloadConditions(
            allowsCellularAccess: true,
            allowsBackgroundDownloading: true
        )
        let notificationCenter = NotificationCenter.default
        activeDownloadCallback = callback
        activeDownloadModelId = modelId
        successObserver = notificationCenter.addObserver(
            forName: .mlkitModelDownloadDidSucceed,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.completeDownload(from: notification, error: nil)
        }
        failureObserver = notificationCenter.addObserver(
            forName: .mlkitModelDownloadDidFail,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            let error = notification.userInfo?[ModelDownloadUserInfoKey.error] as? NSError
            self?.completeDownload(from: notification, error: error)
        }
        activeDownloadProgress = ModelManager.modelManager().download(targetModel, conditions: conditions)
    }

    func cancelModelDownload(modelId: String) {
        guard activeDownloadModelId == modelId else { return }
        activeDownloadProgress?.cancel()
        activeDownloadCallback?.complete(value: false, errorMessage: "Model download canceled.")
        clearActiveDownload()
    }

    func deleteModel(modelId: String, callback: IosBooleanCallback) {
        guard isKnownModel(modelId) else {
            callback.complete(value: false, errorMessage: "Translation model was not found.")
            return
        }

        cancelModelDownload(modelId: modelId)
        ModelManager.modelManager().deleteDownloadedModel(targetModel) { error in
            callback.complete(
                value: error == nil,
                errorMessage: error?.localizedDescription
            )
        }
    }

    func translate(text: String, modelId: String, callback: IosStringCallback) {
        guard isKnownModel(modelId) else {
            callback.complete(value: nil, errorMessage_: "Translation model was not found.")
            return
        }

        translator.translate(text) { translatedText, error in
            callback.complete(
                value: translatedText,
                errorMessage_: error?.localizedDescription
            )
        }
    }

    private func isKnownModel(_ modelId: String) -> Bool {
        modelId == self.modelId
    }

    private func completeDownload(from notification: Notification, error: NSError?) {
        guard isTargetModelNotification(notification) else { return }

        let callback = activeDownloadCallback
        clearActiveDownload()
        callback?.complete(
            value: error == nil,
            errorMessage: error?.localizedDescription
        )
    }

    private func isTargetModelNotification(_ notification: Notification) -> Bool {
        guard let model = notification.userInfo?[ModelDownloadUserInfoKey.remoteModel] as? TranslateRemoteModel else {
            return false
        }
        return model.language == targetModel.language
    }

    private func clearActiveDownload() {
        let notificationCenter = NotificationCenter.default
        if let successObserver {
            notificationCenter.removeObserver(successObserver)
        }
        if let failureObserver {
            notificationCenter.removeObserver(failureObserver)
        }

        activeDownloadProgress = nil
        activeDownloadCallback = nil
        activeDownloadModelId = nil
        successObserver = nil
        failureObserver = nil
    }
}
