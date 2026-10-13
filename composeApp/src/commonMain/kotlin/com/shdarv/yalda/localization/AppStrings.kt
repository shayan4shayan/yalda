package com.shdarv.yalda.localization

import androidx.compose.runtime.staticCompositionLocalOf

data class TranslationRow(
    val en: String,
    val es: String,
    val fr: String,
    val de: String,
    val ja: String,
    val fa: String,
    val it: String,
    val pt: String
) {
    fun value(languageCode: String): String = when (languageCode) {
        "es" -> es
        "fr" -> fr
        "de" -> de
        "ja" -> ja
        "fa" -> fa
        "it" -> it
        "pt" -> pt
        else -> en
    }
}

class AppStrings internal constructor(val languageCode: String) {
    operator fun invoke(key: String, vararg arguments: Any?): String {
        val template = AppStringCatalog.rowsByKey[key]?.value(languageCode) ?: key
        return arguments.foldIndexed(template) { index, value, argument ->
            value.replace("{$index}", argument?.toString().orEmpty())
        }
    }

    fun languageName(code: String): String = invoke(
        when (code) {
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "ja" -> "Japanese"
            "fa" -> "Persian"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            else -> "English"
        }
    )

    fun runtime(message: String): String {
        if (AppStringCatalog.rowsByKey.containsKey(message)) return invoke(message)

        Regex("^(.+) language pack was removed\\.$").matchEntire(message)?.let { match ->
            return invoke("{0} language pack was removed.", invoke(match.groupValues[1]))
        }
        Regex("^(.+) does not match this profile's directed language pair\\.$").matchEntire(message)?.let { match ->
            return invoke("{0} does not match this profile's directed language pair.", match.groupValues[1])
        }
        Regex("^Import failed for ([^:]+)(?:: .*)?$").matchEntire(message)?.let { match ->
            return invoke("Import failed for {0}.", match.groupValues[1])
        }
        Regex("^(.+): (\\d+) added, (\\d+) updated, (\\d+) edits preserved, (\\d+) collisions skipped\\.$")
            .matchEntire(message)?.let { match ->
                return invoke(
                    "{0}: {1} added, {2} updated, {3} edits preserved, {4} collisions skipped.",
                    match.groupValues[1],
                    match.groupValues[2],
                    match.groupValues[3],
                    match.groupValues[4],
                    match.groupValues[5]
                )
            }

        return when {
            message.startsWith("Preparing ") -> invoke("Preparing offline translation...")
            message.startsWith("Downloading ") && message.endsWith(" language packs...") ->
                invoke("Downloading language packs...")
            message.endsWith(" is ready offline.") -> invoke("Offline translation is ready.")
            else -> message
        }
    }
}

object AppStringCatalog {
    val supportedLanguageCodes = setOf("en", "es", "fr", "de", "ja", "fa", "it", "pt")

    val rows: List<TranslationRow> = listOf(
        row("Yalda", "Yalda", "Yalda", "Yalda", "Yalda", "یلدا", "Yalda", "Yalda"),
        row("Search", "Buscar", "Rechercher", "Suchen", "検索", "جستجو", "Cerca", "Pesquisar"),
        row("Quiz", "Cuestionario", "Quiz", "Quiz", "クイズ", "آزمون", "Quiz", "Questionário"),
        row("Menu", "Menú", "Menu", "Menü", "メニュー", "منو", "Menu", "Menu"),
        row("Learn", "Aprender", "Apprendre", "Lernen", "学習", "یادگیری", "Impara", "Aprender"),
        row("Import", "Importar", "Importer", "Importieren", "インポート", "وارد کردن", "Importa", "Importar"),
        row("Settings", "Ajustes", "Réglages", "Einstellungen", "設定", "تنظیمات", "Impostazioni", "Definições"),
        row("About us", "Acerca de", "À propos", "Über uns", "アプリについて", "درباره ما", "Informazioni", "Sobre nós"),
        row("Home", "Inicio", "Accueil", "Start", "ホーム", "خانه", "Home", "Início"),
        row("Translate", "Traducir", "Traduire", "Übersetzen", "翻訳", "ترجمه", "Traduci", "Traduzir"),
        row("Text to translate", "Texto para traducir", "Texte à traduire", "Text zum Übersetzen", "翻訳するテキスト", "متن برای ترجمه", "Testo da tradurre", "Texto a traduzir"),
        row("Add to category", "Añadir a categoría", "Ajouter à une catégorie", "Zur Kategorie hinzufügen", "カテゴリに追加", "افزودن به دسته", "Aggiungi a categoria", "Adicionar a categoria"),
        row("Word saved.", "Palabra guardada.", "Mot enregistré.", "Wort gespeichert.", "単語を保存しました。", "واژه ذخیره شد.", "Parola salvata.", "Palavra guardada."),
        row("Could not save word. Please try again.", "No se pudo guardar la palabra. Inténtalo de nuevo.", "Impossible d'enregistrer le mot. Réessayez.", "Wort konnte nicht gespeichert werden. Bitte erneut versuchen.", "単語を保存できませんでした。もう一度お試しください。", "ذخیره واژه انجام نشد. دوباره تلاش کنید.", "Impossibile salvare la parola. Riprova.", "Não foi possível guardar a palavra. Tente novamente."),
        row("Search words", "Buscar palabras", "Rechercher des mots", "Wörter suchen", "単語を検索", "جستجوی واژه‌ها", "Cerca parole", "Pesquisar palavras"),
        row("Back", "Atrás", "Retour", "Zurück", "戻る", "بازگشت", "Indietro", "Voltar"),
        row("Back to menu", "Volver al menú", "Retour au menu", "Zurück zum Menü", "メニューに戻る", "بازگشت به منو", "Torna al menu", "Voltar ao menu"),
        row("Cancel", "Cancelar", "Annuler", "Abbrechen", "キャンセル", "لغو", "Annulla", "Cancelar"),
        row("Save", "Guardar", "Enregistrer", "Speichern", "保存", "ذخیره", "Salva", "Guardar"),
        row("Delete", "Eliminar", "Supprimer", "Löschen", "削除", "حذف", "Elimina", "Eliminar"),
        row("Add", "Añadir", "Ajouter", "Hinzufügen", "追加", "افزودن", "Aggiungi", "Adicionar"),
        row("Update", "Actualizar", "Mettre à jour", "Aktualisieren", "更新", "به‌روزرسانی", "Aggiorna", "Atualizar"),
        row("Legacy", "Heredado", "Ancien format", "Altes Format", "旧形式", "قدیمی", "Formato precedente", "Formato antigo"),
        row("Continue", "Continuar", "Continuer", "Weiter", "続ける", "ادامه", "Continua", "Continuar"),
        row("Dismiss", "Cerrar", "Fermer", "Schließen", "閉じる", "بستن", "Chiudi", "Fechar"),
        row("Translation download canceled.", "Descarga de traducción cancelada.", "Téléchargement de traduction annulé.", "Übersetzungsdownload abgebrochen.", "翻訳のダウンロードをキャンセルしました。", "بارگیری ترجمه لغو شد.", "Download della traduzione annullato.", "Transferência da tradução cancelada."),
        row("Language pack action canceled.", "Acción del paquete de idioma cancelada.", "Action sur le pack de langue annulée.", "Sprachpaket-Aktion abgebrochen.", "言語パックの操作をキャンセルしました。", "عملیات بسته زبان لغو شد.", "Azione sul pacchetto lingua annullata.", "Ação do pacote de idioma cancelada."),
        row("Preparing offline translation...", "Preparando la traducción sin conexión…", "Préparation de la traduction hors ligne…", "Offline-Übersetzung wird vorbereitet…", "オフライン翻訳を準備中…", "در حال آماده‌سازی ترجمه آفلاین…", "Preparazione della traduzione offline…", "A preparar a tradução offline…"),
        row("Downloading language packs...", "Descargando paquetes de idiomas…", "Téléchargement des packs de langues…", "Sprachpakete werden heruntergeladen…", "言語パックをダウンロード中…", "در حال بارگیری بسته‌های زبان…", "Download dei pacchetti lingua…", "A transferir pacotes de idiomas…"),
        row("Offline translation is ready.", "La traducción sin conexión está lista.", "La traduction hors ligne est prête.", "Die Offline-Übersetzung ist bereit.", "オフライン翻訳の準備ができました。", "ترجمه آفلاین آماده است.", "La traduzione offline è pronta.", "A tradução offline está pronta."),
        row("{0} language pack was removed.", "Se eliminó el paquete de {0}.", "Le pack {0} a été supprimé.", "Das Sprachpaket {0} wurde entfernt.", "{0}の言語パックを削除しました。", "بسته زبان {0} حذف شد.", "Il pacchetto {0} è stato rimosso.", "O pacote de {0} foi removido."),
        row("Offline translation is not available on this device.", "La traducción sin conexión no está disponible en este dispositivo.", "La traduction hors ligne n'est pas disponible sur cet appareil.", "Offline-Übersetzung ist auf diesem Gerät nicht verfügbar.", "この端末ではオフライン翻訳を利用できません。", "ترجمه آفلاین در این دستگاه در دسترس نیست.", "La traduzione offline non è disponibile su questo dispositivo.", "A tradução offline não está disponível neste dispositivo."),
        row("Offline translation is not available on this platform.", "La traducción sin conexión no está disponible en esta plataforma.", "La traduction hors ligne n'est pas disponible sur cette plateforme.", "Offline-Übersetzung ist auf dieser Plattform nicht verfügbar.", "このプラットフォームではオフライン翻訳を利用できません。", "ترجمه آفلاین در این پلتفرم در دسترس نیست.", "La traduzione offline non è disponibile su questa piattaforma.", "A tradução offline não está disponível nesta plataforma."),
        row("Enter a word to translate.", "Introduce una palabra para traducir.", "Saisissez un mot à traduire.", "Gib ein Wort zum Übersetzen ein.", "翻訳する単語を入力してください。", "واژه‌ای برای ترجمه وارد کنید.", "Inserisci una parola da tradurre.", "Introduza uma palavra para traduzir."),
        row("Next", "Siguiente", "Suivant", "Weiter", "次へ", "بعدی", "Avanti", "Seguinte"),
        row("Check", "Comprobar", "Vérifier", "Prüfen", "確認", "بررسی", "Controlla", "Verificar"),
        row("Play", "Reproducir", "Écouter", "Abspielen", "再生", "پخش", "Riproduci", "Reproduzir"),
        row("Apply", "Aplicar", "Appliquer", "Anwenden", "適用", "اعمال", "Applica", "Aplicar"),
        row("Sample", "Muestra", "Exemple", "Beispiel", "サンプル", "نمونه", "Esempio", "Exemplo"),
        row("Remove", "Quitar", "Retirer", "Entfernen", "削除", "برداشتن", "Rimuovi", "Remover"),
        row("Allow", "Permitir", "Autoriser", "Erlauben", "許可", "اجازه دادن", "Consenti", "Permitir"),
        row("On", "Activado", "Activé", "Ein", "オン", "روشن", "Attivo", "Ligado"),
        row("Off", "Desactivado", "Désactivé", "Aus", "オフ", "خاموش", "Disattivo", "Desligado"),
        row("Your Library", "Tu biblioteca", "Votre bibliothèque", "Deine Bibliothek", "あなたのライブラリ", "کتابخانه شما", "La tua raccolta", "A sua biblioteca"),
        row("Switch profile", "Cambiar perfil", "Changer de profil", "Profil wechseln", "プロフィールを切り替える", "تغییر نمایه", "Cambia profilo", "Mudar perfil"),
        row("Profiles", "Perfiles", "Profils", "Profile", "プロフィール", "نمایه‌ها", "Profili", "Perfis"),
        row("Selected", "Seleccionado", "Sélectionné", "Ausgewählt", "選択済み", "انتخاب‌شده", "Selezionato", "Selecionado"),
        row("Rename", "Renombrar", "Renommer", "Umbenennen", "名前を変更", "تغییر نام", "Rinomina", "Mudar nome"),
        row("Create profile", "Crear perfil", "Créer un profil", "Profil erstellen", "プロフィールを作成", "ساخت نمایه", "Crea profilo", "Criar perfil"),
        row("Rename profile", "Renombrar perfil", "Renommer le profil", "Profil umbenennen", "プロフィール名を変更", "تغییر نام نمایه", "Rinomina profilo", "Mudar nome do perfil"),
        row("Delete {0}?", "¿Eliminar {0}?", "Supprimer {0} ?", "{0} löschen?", "{0}を削除しますか？", "{0} حذف شود؟", "Eliminare {0}?", "Eliminar {0}?"),
        row("Its independent categories, words, and setup selections will be deleted.", "Se eliminarán sus categorías, palabras y opciones de configuración independientes.", "Ses catégories, mots et choix de configuration indépendants seront supprimés.", "Die unabhängigen Kategorien, Wörter und Einstellungen werden gelöscht.", "このプロフィールのカテゴリ、単語、設定が削除されます。", "دسته‌ها، واژه‌ها و انتخاب‌های تنظیمات مستقل آن حذف می‌شوند.", "Le categorie, le parole e le impostazioni indipendenti verranno eliminate.", "As categorias, palavras e opções de configuração independentes serão eliminadas."),
        row("At least one profile is required.", "Se necesita al menos un perfil.", "Au moins un profil est requis.", "Mindestens ein Profil ist erforderlich.", "少なくとも1つのプロフィールが必要です。", "حداقل یک نمایه لازم است.", "È necessario almeno un profilo.", "É necessário pelo menos um perfil."),
        row("{0} of 3", "{0} de 3", "{0} sur 3", "{0} von 3", "3段階中{0}", "{0} از ۳", "{0} di 3", "{0} de 3"),
        row("Choose languages", "Elegir idiomas", "Choisir les langues", "Sprachen auswählen", "言語を選択", "انتخاب زبان‌ها", "Scegli le lingue", "Escolher idiomas"),
        row("Choose an exact source and target. They cannot be the same.", "Elige un idioma de origen y destino distintos.", "Choisissez des langues source et cible différentes.", "Wähle unterschiedliche Ausgangs- und Zielsprachen.", "異なる翻訳元と翻訳先の言語を選んでください。", "زبان مبدأ و مقصد متفاوت انتخاب کنید.", "Scegli lingue di origine e destinazione diverse.", "Escolha idiomas de origem e destino diferentes."),
        row("Source language", "Idioma de origen", "Langue source", "Ausgangssprache", "翻訳元の言語", "زبان مبدأ", "Lingua di origine", "Idioma de origem"),
        row("Target language", "Idioma de destino", "Langue cible", "Zielsprache", "翻訳先の言語", "زبان مقصد", "Lingua di destinazione", "Idioma de destino"),
        row("English", "Inglés", "Anglais", "Englisch", "英語", "انگلیسی", "Inglese", "Inglês"),
        row("Spanish", "Español", "Espagnol", "Spanisch", "スペイン語", "اسپانیایی", "Spagnolo", "Espanhol"),
        row("French", "Francés", "Français", "Französisch", "フランス語", "فرانسوی", "Francese", "Francês"),
        row("German", "Alemán", "Allemand", "Deutsch", "ドイツ語", "آلمانی", "Tedesco", "Alemão"),
        row("Japanese", "Japonés", "Japonais", "Japanisch", "日本語", "ژاپنی", "Giapponese", "Japonês"),
        row("Persian", "Persa", "Persan", "Persisch", "ペルシャ語", "فارسی", "Persiano", "Persa"),
        row("Italian", "Italiano", "Italien", "Italienisch", "イタリア語", "ایتالیایی", "Italiano", "Italiano"),
        row("Portuguese", "Portugués", "Portugais", "Portugiesisch", "ポルトガル語", "پرتغالی", "Portoghese", "Português"),
        row("{0} to {1}", "{0} a {1}", "{0} vers {1}", "{0} nach {1}", "{0}から{1}", "{0} به {1}", "Da {0} a {1}", "{0} para {1}"),
        row("Choose category packs", "Elegir paquetes de categorías", "Choisir les packs de catégories", "Kategoriepakete auswählen", "カテゴリパックを選択", "انتخاب بسته‌های دسته‌بندی", "Scegli i pacchetti di categorie", "Escolher pacotes de categorias"),
        row("Recommended packs are preselected. Nothing is imported until you use Import.", "Los paquetes recomendados están preseleccionados. Nada se importa hasta que uses Importar.", "Les packs recommandés sont présélectionnés. Rien n'est importé avant l'action Importer.", "Empfohlene Pakete sind vorausgewählt. Erst mit Importieren werden Daten übernommen.", "おすすめのパックは選択済みです。インポートを実行するまで追加されません。", "بسته‌های پیشنهادی از پیش انتخاب شده‌اند. تا زمانی که «وارد کردن» را نزنید چیزی وارد نمی‌شود.", "I pacchetti consigliati sono preselezionati. Nulla viene importato finché non usi Importa.", "Os pacotes recomendados estão pré-selecionados. Nada é importado até usar Importar."),
        row("Loading compatible packs...", "Cargando paquetes compatibles…", "Chargement des packs compatibles…", "Kompatible Pakete werden geladen…", "対応パックを読み込み中…", "در حال بارگیری بسته‌های سازگار…", "Caricamento dei pacchetti compatibili…", "A carregar pacotes compatíveis…"),
        row("No compatible packs are currently available. You can continue with an empty library.", "No hay paquetes compatibles disponibles. Puedes continuar con una biblioteca vacía.", "Aucun pack compatible n'est disponible. Vous pouvez continuer avec une bibliothèque vide.", "Derzeit sind keine kompatiblen Pakete verfügbar. Du kannst mit einer leeren Bibliothek fortfahren.", "対応するパックはありません。空のライブラリで続けられます。", "در حال حاضر بسته سازگاری موجود نیست. می‌توانید با کتابخانه خالی ادامه دهید.", "Non sono disponibili pacchetti compatibili. Puoi continuare con una raccolta vuota.", "Não existem pacotes compatíveis. Pode continuar com uma biblioteca vazia."),
        row("Retry catalog", "Reintentar catálogo", "Réessayer le catalogue", "Katalog erneut laden", "カタログを再試行", "تلاش دوباره برای فهرست", "Riprova catalogo", "Tentar catálogo novamente"),
        row("{0} words · v{1} · {2}", "{0} palabras · v{1} · {2}", "{0} mots · v{1} · {2}", "{0} Wörter · v{1} · {2}", "{0}語 · v{1} · {2}", "{0} واژه · نسخه {1} · {2}", "{0} parole · v{1} · {2}", "{0} palavras · v{1} · {2}"),
        row("Recommended", "Recomendado", "Recommandé", "Empfohlen", "おすすめ", "پیشنهادی", "Consigliato", "Recomendado"),
        row("Offline translation", "Traducción sin conexión", "Traduction hors ligne", "Offline-Übersetzung", "オフライン翻訳", "ترجمه آفلاین", "Traduzione offline", "Tradução offline"),
        row("Estimated storage: up to {0} MB · {1}", "Almacenamiento estimado: hasta {0} MB · {1}", "Stockage estimé : jusqu'à {0} Mo · {1}", "Geschätzter Speicher: bis zu {0} MB · {1}", "推定容量：最大{0} MB · {1}", "فضای تخمینی: تا {0} مگابایت · {1}", "Spazio stimato: fino a {0} MB · {1}", "Armazenamento estimado: até {0} MB · {1}"),
        row("Ready", "Listo", "Prêt", "Bereit", "準備完了", "آماده", "Pronto", "Pronto"),
        row("Not downloaded", "No descargado", "Non téléchargé", "Nicht heruntergeladen", "未ダウンロード", "بارگیری نشده", "Non scaricato", "Não transferido"),
        row("Estimated model storage: up to {0} MB", "Almacenamiento estimado: hasta {0} MB", "Stockage estimé : jusqu'à {0} Mo", "Geschätzter Speicher: bis zu {0} MB", "推定モデル容量：最大{0} MB", "فضای تخمینی مدل: تا {0} مگابایت", "Spazio stimato: fino a {0} MB", "Armazenamento estimado: até {0} MB"),
        row("Prepare offline translation", "Preparar traducción sin conexión", "Préparer la traduction hors ligne", "Offline-Übersetzung vorbereiten", "オフライン翻訳を準備", "آماده‌سازی ترجمه آفلاین", "Prepara traduzione offline", "Preparar tradução offline"),
        row("Save this preference for Settings", "Guardar esta preferencia en Ajustes", "Enregistrer cette préférence dans Réglages", "Diese Auswahl in den Einstellungen speichern", "この設定を保存", "ذخیره این ترجیح برای تنظیمات", "Salva questa preferenza nelle Impostazioni", "Guardar esta preferência nas Definições"),
        row("Use Wi-Fi when downloading models. Profile creation will not download anything.", "Usa Wi-Fi para descargar modelos. Crear el perfil no descargará nada.", "Utilisez le Wi-Fi pour télécharger les modèles. La création du profil ne télécharge rien.", "Nutze WLAN zum Herunterladen. Beim Erstellen des Profils wird nichts geladen.", "モデルのダウンロードにはWi-Fiを使用してください。プロフィール作成時にはダウンロードされません。", "برای بارگیری مدل‌ها از وای‌فای استفاده کنید. ساخت نمایه چیزی بارگیری نمی‌کند.", "Usa il Wi-Fi per scaricare i modelli. La creazione del profilo non scarica nulla.", "Use Wi-Fi para transferir modelos. A criação do perfil não transfere nada."),
        row("{0} dataset pack(s) selected. Importing remains a separate action.", "{0} paquete(s) seleccionado(s). La importación sigue siendo una acción separada.", "{0} pack(s) sélectionné(s). L'importation reste une action distincte.", "{0} Datensatzpaket(e) ausgewählt. Der Import bleibt ein eigener Schritt.", "{0}個のデータセットパックを選択しました。インポートは別の操作です。", "{0} بسته داده انتخاب شده است. وارد کردن همچنان عملی جداگانه است.", "{0} pacchetto/i selezionato/i. L'importazione resta un'azione separata.", "{0} pacote(s) selecionado(s). A importação continua a ser uma ação separada."),
        row("This pair may use English as an intermediary and can have lower translation quality.", "Este par puede usar el inglés como intermediario y tener menor calidad.", "Cette paire peut utiliser l'anglais comme intermédiaire et offrir une qualité moindre.", "Dieses Paar kann Englisch als Zwischensprache nutzen und eine geringere Qualität haben.", "この言語ペアは英語を経由するため、翻訳品質が低下する場合があります。", "این جفت ممکن است از انگلیسی به‌عنوان واسطه استفاده کند و کیفیت کمتری داشته باشد.", "Questa coppia può usare l'inglese come intermediario e avere una qualità inferiore.", "Este par pode usar o inglês como intermediário e ter qualidade inferior."),
        row("Skip & continue", "Omitir y continuar", "Ignorer et continuer", "Überspringen und weiter", "スキップして続ける", "رد کردن و ادامه", "Salta e continua", "Ignorar e continuar"),
        row("Library", "Biblioteca", "Bibliothèque", "Bibliothek", "ライブラリ", "کتابخانه", "Raccolta", "Biblioteca"),
        row("Search categories and words", "Buscar categorías y palabras", "Rechercher des catégories et des mots", "Kategorien und Wörter suchen", "カテゴリと単語を検索", "جستجوی دسته‌ها و واژه‌ها", "Cerca categorie e parole", "Pesquisar categorias e palavras"),
        row("Clear search", "Borrar búsqueda", "Effacer la recherche", "Suche löschen", "検索を消去", "پاک کردن جستجو", "Cancella ricerca", "Limpar pesquisa"),
        row("Create category", "Crear categoría", "Créer une catégorie", "Kategorie erstellen", "カテゴリを作成", "ساخت دسته", "Crea categoria", "Criar categoria"),
        row("Add word", "Añadir palabra", "Ajouter un mot", "Wort hinzufügen", "単語を追加", "افزودن واژه", "Aggiungi parola", "Adicionar palavra"),
        row("Categories", "Categorías", "Catégories", "Kategorien", "カテゴリ", "دسته‌ها", "Categorie", "Categorias"),
        row("Search categories", "Buscar categorías", "Rechercher des catégories", "Kategorien suchen", "カテゴリを検索", "جستجوی دسته‌ها", "Cerca categorie", "Pesquisar categorias"),
        row("Add Category", "Añadir categoría", "Ajouter une catégorie", "Kategorie hinzufügen", "カテゴリを追加", "افزودن دسته", "Aggiungi categoria", "Adicionar categoria"),
        row("Add category", "Añadir categoría", "Ajouter une catégorie", "Kategorie hinzufügen", "カテゴリを追加", "افزودن دسته", "Aggiungi categoria", "Adicionar categoria"),
        row("No categories yet. Add your first category.", "Aún no hay categorías. Añade la primera.", "Aucune catégorie. Ajoutez la première.", "Noch keine Kategorien. Füge deine erste hinzu.", "カテゴリはまだありません。最初のカテゴリを追加してください。", "هنوز دسته‌ای نیست. نخستین دسته را اضافه کنید.", "Nessuna categoria. Aggiungi la prima.", "Ainda não existem categorias. Adicione a primeira."),
        row("Words", "Palabras", "Mots", "Wörter", "単語", "واژه‌ها", "Parole", "Palavras"),
        row("All Words", "Todas las palabras", "Tous les mots", "Alle Wörter", "すべての単語", "همه واژه‌ها", "Tutte le parole", "Todas as palavras"),
        row("{0} words", "{0} palabras", "{0} mots", "{0} Wörter", "{0}語", "{0} واژه", "{0} parole", "{0} palavras"),
        row("Words in {0}", "Palabras en {0}", "Mots dans {0}", "Wörter in {0}", "{0}の単語", "واژه‌های {0}", "Parole in {0}", "Palavras em {0}"),
        row("Show all words", "Mostrar todas las palabras", "Afficher tous les mots", "Alle Wörter anzeigen", "すべての単語を表示", "نمایش همه واژه‌ها", "Mostra tutte le parole", "Mostrar todas as palavras"),
        row("No categories yet. Search for a name to create one.", "Aún no hay categorías. Busca un nombre para crear una.", "Aucune catégorie. Recherchez un nom pour en créer une.", "Noch keine Kategorien. Suche nach einem Namen, um eine zu erstellen.", "カテゴリはまだありません。名前を検索して作成できます。", "هنوز دسته‌ای وجود ندارد. برای ساخت، نامی را جستجو کنید.", "Nessuna categoria. Cerca un nome per crearne una.", "Ainda não existem categorias. Pesquise um nome para criar uma."),
        row("No categories match “{0}”.", "Ninguna categoría coincide con «{0}».", "Aucune catégorie ne correspond à « {0} ».", "Keine Kategorie entspricht „{0}“.", "「{0}」に一致するカテゴリはありません。", "هیچ دسته‌ای با «{0}» مطابقت ندارد.", "Nessuna categoria corrisponde a “{0}”.", "Nenhuma categoria corresponde a «{0}»."),
        row("No words yet. Search for a word to add it.", "Aún no hay palabras. Busca una para añadirla.", "Aucun mot. Recherchez un mot pour l'ajouter.", "Noch keine Wörter. Suche nach einem Wort, um es hinzuzufügen.", "単語はまだありません。検索して追加できます。", "هنوز واژه‌ای وجود ندارد. برای افزودن، واژه‌ای را جستجو کنید.", "Nessuna parola. Cercane una per aggiungerla.", "Ainda não existem palavras. Pesquise uma para a adicionar."),
        row("No words match “{0}”.", "Ninguna palabra coincide con «{0}».", "Aucun mot ne correspond à « {0} ».", "Kein Wort entspricht „{0}“.", "「{0}」に一致する単語はありません。", "هیچ واژه‌ای با «{0}» مطابقت ندارد.", "Nessuna parola corrisponde a “{0}”.", "Nenhuma palavra corresponde a «{0}»."),
        row("No matching words in {0}.", "No hay palabras coincidentes en {0}.", "Aucun mot correspondant dans {0}.", "Keine passenden Wörter in {0}.", "{0}に一致する単語はありません。", "واژه مطابقی در {0} وجود ندارد.", "Nessuna parola corrispondente in {0}.", "Não existem palavras correspondentes em {0}."),
        row("Unknown category", "Categoría desconocida", "Catégorie inconnue", "Unbekannte Kategorie", "不明なカテゴリ", "دسته ناشناخته", "Categoria sconosciuta", "Categoria desconhecida"),
        row("Create Category", "Crear categoría", "Créer une catégorie", "Kategorie erstellen", "カテゴリを作成", "ساخت دسته", "Crea categoria", "Criar categoria"),
        row("Edit Category", "Editar categoría", "Modifier la catégorie", "Kategorie bearbeiten", "カテゴリを編集", "ویرایش دسته", "Modifica categoria", "Editar categoria"),
        row("Delete Category", "Eliminar categoría", "Supprimer la catégorie", "Kategorie löschen", "カテゴリを削除", "حذف دسته", "Elimina categoria", "Eliminar categoria"),
        row("Delete {0} and all its words?", "¿Eliminar {0} y todas sus palabras?", "Supprimer {0} et tous ses mots ?", "{0} und alle zugehörigen Wörter löschen?", "{0}とそのすべての単語を削除しますか？", "{0} و همه واژه‌هایش حذف شوند؟", "Eliminare {0} e tutte le sue parole?", "Eliminar {0} e todas as suas palavras?"),
        row("Add Word", "Añadir palabra", "Ajouter un mot", "Wort hinzufügen", "単語を追加", "افزودن واژه", "Aggiungi parola", "Adicionar palavra"),
        row("Edit Word", "Editar palabra", "Modifier le mot", "Wort bearbeiten", "単語を編集", "ویرایش واژه", "Modifica parola", "Editar palavra"),
        row("Delete Word", "Eliminar palabra", "Supprimer le mot", "Wort löschen", "単語を削除", "حذف واژه", "Elimina parola", "Eliminar palavra"),
        row("{0} item", "{0} elemento", "{0} élément", "{0} Element", "{0}件", "{0} مورد", "{0} elemento", "{0} item"),
        row("{0} items", "{0} elementos", "{0} éléments", "{0} Elemente", "{0}件", "{0} مورد", "{0} elementi", "{0} itens"),
        row("Show less", "Mostrar menos", "Afficher moins", "Weniger anzeigen", "表示を減らす", "نمایش کمتر", "Mostra meno", "Mostrar menos"),
        row("See all", "Ver todo", "Tout afficher", "Alle anzeigen", "すべて表示", "نمایش همه", "Vedi tutto", "Ver tudo"),
        row("Word", "Palabra", "Mot", "Wort", "単語", "واژه", "Parola", "Palavra"),
        row("Meaning", "Significado", "Signification", "Bedeutung", "意味", "معنی", "Significato", "Significado"),
        row("Description (optional)", "Descripción (opcional)", "Description (facultative)", "Beschreibung (optional)", "説明（任意）", "توضیح (اختیاری)", "Descrizione (facoltativa)", "Descrição (opcional)"),
        row("Translate word", "Traducir palabra", "Traduire le mot", "Wort übersetzen", "単語を翻訳", "ترجمه واژه", "Traduci parola", "Traduzir palavra"),
        row("Powered by Google Translate", "Con la tecnología de Google Translate", "Fourni par Google Traduction", "Unterstützt von Google Übersetzer", "Google 翻訳を使用", "با پشتیبانی مترجم گوگل", "Con tecnologia Google Traduttore", "Com tecnologia Google Tradutor"),
        row("Select category", "Seleccionar categoría", "Sélectionner une catégorie", "Kategorie auswählen", "カテゴリを選択", "انتخاب دسته", "Seleziona categoria", "Selecionar categoria"),
        row("Category", "Categoría", "Catégorie", "Kategorie", "カテゴリ", "دسته", "Categoria", "Categoria"),
        row("Download this profile's offline translation setup in Settings.", "Descarga la traducción sin conexión de este perfil en Ajustes.", "Téléchargez la traduction hors ligne de ce profil dans Réglages.", "Lade die Offline-Übersetzung dieses Profils in den Einstellungen herunter.", "設定でこのプロフィールのオフライン翻訳をダウンロードしてください。", "تنظیم ترجمه آفلاین این نمایه را در تنظیمات بارگیری کنید.", "Scarica la traduzione offline di questo profilo nelle Impostazioni.", "Transfira a tradução offline deste perfil nas Definições."),
        row("Offline translation is unavailable for this language pair.", "La traducción sin conexión no está disponible para este par.", "La traduction hors ligne n'est pas disponible pour cette paire.", "Offline-Übersetzung ist für dieses Sprachpaar nicht verfügbar.", "この言語ペアではオフライン翻訳を利用できません。", "ترجمه آفلاین برای این جفت زبان در دسترس نیست.", "La traduzione offline non è disponibile per questa coppia.", "A tradução offline não está disponível para este par."),
        row("Select a profile before using translation.", "Selecciona un perfil antes de traducir.", "Sélectionnez un profil avant de traduire.", "Wähle vor der Übersetzung ein Profil aus.", "翻訳する前にプロフィールを選択してください。", "پیش از ترجمه یک نمایه انتخاب کنید.", "Seleziona un profilo prima di tradurre.", "Selecione um perfil antes de traduzir."),
        row("All categories", "Todas las categorías", "Toutes les catégories", "Alle Kategorien", "すべてのカテゴリ", "همه دسته‌ها", "Tutte le categorie", "Todas as categorias"),
        row("Choice", "Opciones", "Choix", "Auswahl", "選択", "چندگزینه‌ای", "Scelta", "Escolha"),
        row("Spelling", "Ortografía", "Orthographe", "Rechtschreibung", "スペル", "املا", "Ortografia", "Ortografia"),
        row("Speaking", "Hablar", "Expression orale", "Sprechen", "スピーキング", "گفتاری", "Conversazione", "Fala"),
        row("Select a profile", "Seleccionar un perfil", "Sélectionner un profil", "Profil auswählen", "プロフィールを選択", "انتخاب نمایه", "Seleziona un profilo", "Selecionar um perfil"),
        row("Choose or create a profile before starting a quiz.", "Elige o crea un perfil antes de iniciar un cuestionario.", "Choisissez ou créez un profil avant de commencer un quiz.", "Wähle oder erstelle vor dem Quiz ein Profil.", "クイズを始める前にプロフィールを選択または作成してください。", "پیش از شروع آزمون یک نمایه انتخاب یا ایجاد کنید.", "Scegli o crea un profilo prima del quiz.", "Escolha ou crie um perfil antes de iniciar um questionário."),
        row("More words needed", "Se necesitan más palabras", "Plus de mots requis", "Mehr Wörter erforderlich", "単語が足りません", "واژه‌های بیشتری لازم است", "Servono più parole", "São necessárias mais palavras"),
        row("Add at least 4 words in this category to start multiple choice.", "Añade al menos 4 palabras para iniciar las opciones.", "Ajoutez au moins 4 mots pour commencer le choix multiple.", "Füge mindestens 4 Wörter für die Auswahl hinzu.", "選択問題を始めるには4語以上追加してください。", "برای شروع چندگزینه‌ای حداقل ۴ واژه اضافه کنید.", "Aggiungi almeno 4 parole per iniziare la scelta multipla.", "Adicione pelo menos 4 palavras para iniciar a escolha múltipla."),
        row("Multiple choice", "Opción múltiple", "Choix multiple", "Mehrfachauswahl", "選択問題", "چندگزینه‌ای", "Scelta multipla", "Escolha múltipla"),
        row("Choose the matching meaning.", "Elige el significado correspondiente.", "Choisissez la signification correspondante.", "Wähle die passende Bedeutung.", "一致する意味を選んでください。", "معنی مرتبط را انتخاب کنید.", "Scegli il significato corrispondente.", "Escolha o significado correspondente."),
        row("Correct.", "Correcto.", "Correct.", "Richtig.", "正解です。", "درست است.", "Corretto.", "Correto."),
        row("Not quite. Try another answer.", "No del todo. Prueba otra respuesta.", "Pas tout à fait. Essayez une autre réponse.", "Nicht ganz. Versuch eine andere Antwort.", "惜しいです。別の答えを試してください。", "کاملاً درست نیست. پاسخ دیگری را امتحان کنید.", "Non proprio. Prova un'altra risposta.", "Ainda não. Tente outra resposta."),
        row("No words yet", "Aún no hay palabras", "Aucun mot", "Noch keine Wörter", "単語はまだありません", "هنوز واژه‌ای نیست", "Nessuna parola", "Ainda não existem palavras"),
        row("Add words to this category before starting spelling practice.", "Añade palabras antes de practicar ortografía.", "Ajoutez des mots avant l'exercice d'orthographe.", "Füge vor der Rechtschreibübung Wörter hinzu.", "スペル練習の前に単語を追加してください。", "پیش از تمرین املا واژه اضافه کنید.", "Aggiungi parole prima dell'esercizio di ortografia.", "Adicione palavras antes do exercício de ortografia."),
        row("Listen and type", "Escucha y escribe", "Écoutez et écrivez", "Anhören und tippen", "聞いて入力", "گوش دهید و بنویسید", "Ascolta e scrivi", "Ouça e escreva"),
        row("Meaning: {0}", "Significado: {0}", "Signification : {0}", "Bedeutung: {0}", "意味：{0}", "معنی: {0}", "Significato: {0}", "Significado: {0}"),
        row("Play word", "Reproducir palabra", "Écouter le mot", "Wort abspielen", "単語を再生", "پخش واژه", "Riproduci parola", "Reproduzir palavra"),
        row("Type the word", "Escribe la palabra", "Tapez le mot", "Wort eingeben", "単語を入力", "واژه را بنویسید", "Scrivi la parola", "Escreva a palavra"),
        row("Not quite. It was {0}.", "No del todo. Era {0}.", "Pas tout à fait. C'était {0}.", "Nicht ganz. Es war {0}.", "惜しいです。正解は{0}です。", "کاملاً درست نیست. پاسخ {0} بود.", "Non proprio. Era {0}.", "Ainda não. Era {0}."),
        row("Add words to this category before starting speaking practice.", "Añade palabras antes de practicar conversación.", "Ajoutez des mots avant l'exercice oral.", "Füge vor der Sprechübung Wörter hinzu.", "スピーキング練習の前に単語を追加してください。", "پیش از تمرین گفتاری واژه اضافه کنید.", "Aggiungi parole prima dell'esercizio di conversazione.", "Adicione palavras antes do exercício de fala."),
        row("Speaking unavailable", "Habla no disponible", "Expression orale indisponible", "Sprechen nicht verfügbar", "スピーキングは利用できません", "گفتار در دسترس نیست", "Conversazione non disponibile", "Fala indisponível"),
        row("Speech recognition is not supported on this device.", "Este dispositivo no admite reconocimiento de voz.", "La reconnaissance vocale n'est pas prise en charge sur cet appareil.", "Spracherkennung wird auf diesem Gerät nicht unterstützt.", "この端末では音声認識を利用できません。", "تشخیص گفتار در این دستگاه پشتیبانی نمی‌شود.", "Il riconoscimento vocale non è supportato su questo dispositivo.", "O reconhecimento de voz não é suportado neste dispositivo."),
        row("Say what you hear", "Di lo que escuchas", "Dites ce que vous entendez", "Sprich nach, was du hörst", "聞こえた言葉を話す", "آنچه می‌شنوید بگویید", "Ripeti ciò che senti", "Diga o que ouve"),
        row("Listen first, then record your pronunciation.", "Escucha y luego graba tu pronunciación.", "Écoutez, puis enregistrez votre prononciation.", "Höre zuerst zu und nimm dann deine Aussprache auf.", "まず聞いてから発音を録音してください。", "ابتدا گوش دهید، سپس تلفظ خود را ضبط کنید.", "Ascolta, poi registra la pronuncia.", "Ouça primeiro e depois grave a pronúncia."),
        row("Listening...", "Escuchando…", "Écoute…", "Hört zu…", "聞き取り中…", "در حال شنیدن…", "Ascolto…", "A ouvir…"),
        row("Speak", "Hablar", "Parler", "Sprechen", "話す", "صحبت", "Parla", "Falar"),
        row("Start speaking", "Empezar a hablar", "Commencer à parler", "Sprechen starten", "音声入力を開始", "شروع گفتار", "Inizia a parlare", "Começar a falar"),
        row("Great pronunciation.", "Excelente pronunciación.", "Excellente prononciation.", "Tolle Aussprache.", "素晴らしい発音です。", "تلفظ عالی بود.", "Ottima pronuncia.", "Ótima pronúncia."),
        row("Heard “{0}”. Try again.", "Se oyó «{0}». Inténtalo de nuevo.", "Entendu « {0} ». Réessayez.", "„{0}“ gehört. Versuch es erneut.", "「{0}」と聞こえました。もう一度お試しください。", "«{0}» شنیده شد. دوباره تلاش کنید.", "Ho sentito “{0}”. Riprova.", "Ouvi «{0}». Tente novamente."),
        row("Heard", "Escuchado", "Entendu", "Gehört", "聞き取った内容", "شنیده‌شده", "Sentito", "Ouvido"),
        row("Dataset packs", "Paquetes de datos", "Packs de données", "Datensatzpakete", "データセットパック", "بسته‌های داده", "Pacchetti di dati", "Pacotes de dados"),
        row("Catalog refresh failed. Existing data was kept.", "No se pudo actualizar el catálogo. Se conservaron los datos existentes.", "L'actualisation du catalogue a échoué. Les données existantes ont été conservées.", "Katalogaktualisierung fehlgeschlagen. Vorhandene Daten wurden beibehalten.", "カタログの更新に失敗しました。既存のデータは保持されました。", "تازه‌سازی فهرست ناموفق بود. داده‌های موجود حفظ شدند.", "Aggiornamento del catalogo non riuscito. I dati esistenti sono stati mantenuti.", "A atualização do catálogo falhou. Os dados existentes foram mantidos."),
        row("Showing the last saved catalog. Refresh failed.", "Se muestra el último catálogo guardado. La actualización falló.", "Affichage du dernier catalogue enregistré. L'actualisation a échoué.", "Der zuletzt gespeicherte Katalog wird angezeigt. Aktualisierung fehlgeschlagen.", "最後に保存したカタログを表示しています。更新に失敗しました。", "آخرین فهرست ذخیره‌شده نمایش داده می‌شود. تازه‌سازی ناموفق بود.", "Viene mostrato l'ultimo catalogo salvato. Aggiornamento non riuscito.", "A mostrar o último catálogo guardado. A atualização falhou."),
        row("Using the legacy English to Persian catalog.", "Se usa el catálogo antiguo de inglés a persa.", "Utilisation de l'ancien catalogue anglais-persan.", "Der alte Englisch-Persisch-Katalog wird verwendet.", "旧形式の英語→ペルシャ語カタログを使用しています。", "از فهرست قدیمی انگلیسی به فارسی استفاده می‌شود.", "Viene usato il catalogo precedente inglese-persiano.", "A usar o catálogo antigo de inglês para persa."),
        row("No selected packs are available for this language pair.", "No hay paquetes seleccionados para este par de idiomas.", "Aucun pack sélectionné n'est disponible pour cette paire.", "Für dieses Sprachpaar sind keine ausgewählten Pakete verfügbar.", "この言語ペアで選択済みのパックはありません。", "بسته انتخاب‌شده‌ای برای این جفت زبان موجود نیست.", "Nessun pacchetto selezionato disponibile per questa coppia.", "Não existem pacotes selecionados para este par de idiomas."),
        row("{0} does not match this profile's directed language pair.", "{0} no coincide con el par de idiomas de este perfil.", "{0} ne correspond pas à la paire de langues de ce profil.", "{0} entspricht nicht dem Sprachpaar dieses Profils.", "{0}はこのプロフィールの言語ペアと一致しません。", "{0} با جفت زبان این نمایه مطابقت ندارد.", "{0} non corrisponde alla coppia linguistica di questo profilo.", "{0} não corresponde ao par de idiomas deste perfil."),
        row("Import failed for {0}.", "No se pudo importar {0}.", "Échec de l'importation de {0}.", "Import von {0} fehlgeschlagen.", "{0}のインポートに失敗しました。", "وارد کردن {0} ناموفق بود.", "Importazione di {0} non riuscita.", "Falha ao importar {0}."),
        row("{0}: {1} added, {2} updated, {3} edits preserved, {4} collisions skipped.", "{0}: {1} añadidos, {2} actualizados, {3} cambios conservados y {4} conflictos omitidos.", "{0} : {1} ajoutés, {2} mis à jour, {3} modifications conservées et {4} conflits ignorés.", "{0}: {1} hinzugefügt, {2} aktualisiert, {3} Änderungen beibehalten, {4} Konflikte übersprungen.", "{0}：{1}件追加、{2}件更新、{3}件の編集を保持、{4}件の競合をスキップしました。", "{0}: {1} افزوده، {2} به‌روزرسانی، {3} ویرایش حفظ و {4} تداخل رد شد.", "{0}: {1} aggiunti, {2} aggiornati, {3} modifiche conservate e {4} conflitti ignorati.", "{0}: {1} adicionados, {2} atualizados, {3} edições preservadas e {4} conflitos ignorados."),
        row("Selected during setup appear first.", "Los seleccionados durante la configuración aparecen primero.", "Les packs choisis lors de la configuration apparaissent en premier.", "Bei der Einrichtung ausgewählte Pakete erscheinen zuerst.", "設定時に選択したパックが先に表示されます。", "موارد انتخاب‌شده هنگام راه‌اندازی ابتدا نمایش داده می‌شوند.", "Quelli scelti durante la configurazione appaiono per primi.", "Os selecionados na configuração aparecem primeiro."),
        row("Refresh catalog", "Actualizar catálogo", "Actualiser le catalogue", "Katalog aktualisieren", "カタログを更新", "تازه‌سازی فهرست", "Aggiorna catalogo", "Atualizar catálogo"),
        row("Import selected ({0})", "Importar seleccionados ({0})", "Importer la sélection ({0})", "Ausgewählte importieren ({0})", "選択項目をインポート（{0}）", "وارد کردن انتخاب‌ها ({0})", "Importa selezionati ({0})", "Importar selecionados ({0})"),
        row("Selected in setup", "Seleccionado en la configuración", "Sélectionné lors de la configuration", "Bei der Einrichtung ausgewählt", "設定で選択済み", "در راه‌اندازی انتخاب شده", "Selezionato nella configurazione", "Selecionado na configuração"),
        row("No packs are available for this directed language pair.", "No hay paquetes disponibles para este par de idiomas.", "Aucun pack n'est disponible pour cette paire de langues.", "Für dieses Sprachpaar sind keine Pakete verfügbar.", "この言語ペアで利用できるパックはありません。", "بسته‌ای برای این جفت زبان موجود نیست.", "Nessun pacchetto disponibile per questa coppia.", "Não existem pacotes para este par de idiomas."),
        row("Select a profile to manage its translation setup.", "Selecciona un perfil para gestionar su traducción.", "Sélectionnez un profil pour gérer sa traduction.", "Wähle ein Profil, um die Übersetzung zu verwalten.", "翻訳設定を管理するプロフィールを選択してください。", "برای مدیریت ترجمه یک نمایه انتخاب کنید.", "Seleziona un profilo per gestire la traduzione.", "Selecione um perfil para gerir a tradução."),
        row("Refresh language packs", "Actualizar paquetes de idiomas", "Actualiser les packs de langues", "Sprachpakete aktualisieren", "言語パックを更新", "تازه‌سازی بسته‌های زبان", "Aggiorna pacchetti lingua", "Atualizar pacotes de idiomas"),
        row("Download on Wi-Fi", "Descargar por Wi-Fi", "Télécharger via Wi-Fi", "Über WLAN herunterladen", "Wi-Fiでダウンロード", "بارگیری با وای‌فای", "Scarica tramite Wi-Fi", "Transferir por Wi-Fi"),
        row("Shared language packs", "Paquetes de idiomas compartidos", "Packs de langues partagés", "Gemeinsam genutzte Sprachpakete", "共有言語パック", "بسته‌های زبان مشترک", "Pacchetti lingua condivisi", "Pacotes de idiomas partilhados"),
        row("{0} · about {1} MB", "{0} · unos {1} MB", "{0} · environ {1} Mo", "{0} · etwa {1} MB", "{0} · 約{1} MB", "{0} · حدود {1} مگابایت", "{0} · circa {1} MB", "{0} · cerca de {1} MB"),
        row("Offline preparation was selected during setup. Downloads still start only here.", "La preparación sin conexión se eligió durante la configuración. Las descargas solo comienzan aquí.", "La préparation hors ligne a été choisie. Les téléchargements ne démarrent qu'ici.", "Offline-Vorbereitung wurde gewählt. Downloads starten weiterhin nur hier.", "設定時にオフライン準備を選択しました。ダウンロードはここでのみ開始します。", "آماده‌سازی آفلاین هنگام راه‌اندازی انتخاب شد. بارگیری فقط از اینجا آغاز می‌شود.", "La preparazione offline è stata scelta. I download iniziano solo qui.", "A preparação offline foi escolhida. As transferências só começam aqui."),
        row("Offline preparation was skipped during setup; you can download it now.", "La preparación sin conexión se omitió; puedes descargarla ahora.", "La préparation hors ligne a été ignorée ; vous pouvez la télécharger maintenant.", "Offline-Vorbereitung wurde übersprungen; du kannst sie jetzt laden.", "オフライン準備はスキップされました。今すぐダウンロードできます。", "آماده‌سازی آفلاین رد شد؛ اکنون می‌توانید آن را بارگیری کنید.", "La preparazione offline è stata saltata; puoi scaricarla ora.", "A preparação offline foi ignorada; pode transferi-la agora."),
        row("Offline translation is unavailable on this platform (including the iOS Simulator).", "La traducción sin conexión no está disponible en esta plataforma (incluido el simulador de iOS).", "La traduction hors ligne est indisponible sur cette plateforme (y compris le simulateur iOS).", "Offline-Übersetzung ist auf dieser Plattform nicht verfügbar (einschließlich iOS-Simulator).", "このプラットフォーム（iOSシミュレータを含む）ではオフライン翻訳を利用できません。", "ترجمه آفلاین در این پلتفرم (از جمله شبیه‌ساز iOS) در دسترس نیست.", "La traduzione offline non è disponibile su questa piattaforma (incluso il simulatore iOS).", "A tradução offline não está disponível nesta plataforma (incluindo o simulador iOS)."),
        row("Use Wi-Fi for model downloads. Non-English pairs may use English as an intermediary and can have lower quality.", "Usa Wi-Fi para los modelos. Los pares no ingleses pueden usar el inglés como intermediario y tener menor calidad.", "Utilisez le Wi-Fi. Les paires non anglaises peuvent passer par l'anglais et offrir une qualité moindre.", "Nutze WLAN. Nicht-englische Paare können Englisch als Zwischensprache verwenden und eine geringere Qualität haben.", "モデルのダウンロードにはWi-Fiを使用してください。英語以外のペアは英語を経由し、品質が低下する場合があります。", "برای بارگیری مدل‌ها از وای‌فای استفاده کنید. جفت‌های غیرانگلیسی ممکن است از انگلیسی به‌عنوان واسطه استفاده کنند.", "Usa il Wi-Fi. Le coppie non inglesi possono usare l'inglese come intermediario e avere qualità inferiore.", "Use Wi-Fi. Pares sem inglês podem usar o inglês como intermediário e ter qualidade inferior."),
        row("Delete {0} pack?", "¿Eliminar el paquete de {0}?", "Supprimer le pack {0} ?", "{0}-Paket löschen?", "{0}パックを削除しますか？", "بسته {0} حذف شود؟", "Eliminare il pacchetto {0}?", "Eliminar o pacote {0}?"),
        row("This profile may lose offline translation readiness.", "Este perfil puede perder la traducción sin conexión.", "Ce profil risque de perdre la traduction hors ligne.", "Dieses Profil kann seine Offline-Bereitschaft verlieren.", "このプロフィールでオフライン翻訳を利用できなくなる場合があります。", "این نمایه ممکن است آمادگی ترجمه آفلاین را از دست بدهد.", "Questo profilo potrebbe perdere la traduzione offline.", "Este perfil pode perder a tradução offline."),
        row("Other profiles that will lose offline translation: {0}.", "Otros perfiles que perderán la traducción sin conexión: {0}.", "Autres profils qui perdront la traduction hors ligne : {0}.", "Andere Profile, die Offline-Übersetzung verlieren: {0}.", "オフライン翻訳を利用できなくなる他のプロフィール：{0}。", "نمایه‌های دیگری که ترجمه آفلاین را از دست می‌دهند: {0}.", "Altri profili che perderanno la traduzione offline: {0}.", "Outros perfis que perderão a tradução offline: {0}."),
        row("A vocabulary app for building and reviewing language libraries.", "Una aplicación de vocabulario para crear y repasar bibliotecas de idiomas.", "Une application de vocabulaire pour créer et réviser des bibliothèques linguistiques.", "Eine Vokabel-App zum Erstellen und Wiederholen von Sprachbibliotheken.", "語学ライブラリを作成・復習するための単語アプリです。", "برنامه‌ای برای ساخت و مرور کتابخانه‌های واژگان زبان.", "Un'app di vocabolario per creare e ripassare raccolte linguistiche.", "Uma aplicação de vocabulário para criar e rever bibliotecas de idiomas."),
        row("Offline translations are powered by Google Translate.", "Las traducciones sin conexión funcionan con Google Translate.", "Les traductions hors ligne utilisent Google Traduction.", "Offline-Übersetzungen werden von Google Übersetzer unterstützt.", "オフライン翻訳にはGoogle 翻訳を使用しています。", "ترجمه‌های آفلاین با مترجم گوگل انجام می‌شوند.", "Le traduzioni offline usano Google Traduttore.", "As traduções offline usam o Google Tradutor."),
        row("Non-English language pairs may translate through English and can have lower quality.", "Los pares sin inglés pueden traducirse a través del inglés y tener menor calidad.", "Les paires sans anglais peuvent passer par l'anglais et offrir une qualité moindre.", "Sprachpaare ohne Englisch können über Englisch übersetzen und eine geringere Qualität haben.", "英語以外の言語ペアは英語を経由し、品質が低下する場合があります。", "جفت‌های غیرانگلیسی ممکن است از انگلیسی عبور کنند و کیفیت کمتری داشته باشند.", "Le coppie senza inglese possono passare dall'inglese e avere qualità inferiore.", "Pares sem inglês podem passar pelo inglês e ter qualidade inferior."),
        row("Version {0}", "Versión {0}", "Version {0}", "Version {0}", "バージョン {0}", "نسخه {0}", "Versione {0}", "Versão {0}"),
        row("Daily learning reminder", "Recordatorio diario de aprendizaje", "Rappel d'apprentissage quotidien", "Tägliche Lernerinnerung", "毎日の学習リマインダー", "یادآور روزانه یادگیری", "Promemoria giornaliero", "Lembrete diário de aprendizagem"),
        row("Word reminders are active", "Los recordatorios están activos", "Les rappels sont actifs", "Worterinnerungen sind aktiv", "単語リマインダーは有効です", "یادآورهای واژه فعال هستند", "I promemoria sono attivi", "Os lembretes estão ativos"),
        row("Word reminders are paused", "Los recordatorios están pausados", "Les rappels sont en pause", "Worterinnerungen sind pausiert", "単語リマインダーは一時停止中です", "یادآورهای واژه متوقف هستند", "I promemoria sono in pausa", "Os lembretes estão em pausa"),
        row("Notifications unavailable", "Notificaciones no disponibles", "Notifications indisponibles", "Benachrichtigungen nicht verfügbar", "通知を利用できません", "اعلان‌ها در دسترس نیستند", "Notifiche non disponibili", "Notificações indisponíveis"),
        row("This platform does not support learning reminders.", "Esta plataforma no admite recordatorios de aprendizaje.", "Cette plateforme ne prend pas en charge les rappels.", "Diese Plattform unterstützt keine Lernerinnerungen.", "このプラットフォームは学習リマインダーに対応していません。", "این پلتفرم از یادآورهای یادگیری پشتیبانی نمی‌کند.", "Questa piattaforma non supporta i promemoria.", "Esta plataforma não suporta lembretes de aprendizagem."),
        row("Permission required", "Permiso requerido", "Autorisation requise", "Berechtigung erforderlich", "権限が必要です", "مجوز لازم است", "Autorizzazione necessaria", "Permissão necessária"),
        row("Allow reminder permissions so scheduled words can arrive on time.", "Permite los recordatorios para recibir las palabras a tiempo.", "Autorisez les rappels pour recevoir les mots à l'heure.", "Erlaube Erinnerungen, damit Wörter pünktlich erscheinen.", "予定どおり単語を受け取るには通知を許可してください。", "برای دریافت به‌موقع واژه‌ها، مجوز یادآورها را بدهید.", "Consenti i promemoria per ricevere le parole in tempo.", "Permita lembretes para receber as palavras a tempo."),
        row("Profile required", "Perfil requerido", "Profil requis", "Profil erforderlich", "プロフィールが必要です", "نمایه لازم است", "Profilo necessario", "Perfil necessário"),
        row("Select or create a profile before applying reminders.", "Selecciona o crea un perfil antes de aplicar recordatorios.", "Sélectionnez ou créez un profil avant d'appliquer les rappels.", "Wähle oder erstelle ein Profil, bevor du Erinnerungen anwendest.", "リマインダーを適用する前にプロフィールを選択または作成してください。", "پیش از اعمال یادآورها یک نمایه انتخاب یا ایجاد کنید.", "Seleziona o crea un profilo prima di applicare i promemoria.", "Selecione ou crie um perfil antes de aplicar lembretes."),
        row("Schedule strategy", "Estrategia de horario", "Stratégie de planification", "Zeitplanstrategie", "スケジュール方法", "روش زمان‌بندی", "Strategia di pianificazione", "Estratégia de agendamento"),
        row("Choose when a daily word can be sent", "Elige cuándo enviar una palabra diaria", "Choisissez quand envoyer un mot quotidien", "Wähle, wann ein tägliches Wort gesendet wird", "毎日の単語を送る時間を選択", "زمان ارسال واژه روزانه را انتخاب کنید", "Scegli quando inviare una parola quotidiana", "Escolha quando enviar uma palavra diária"),
        row("Exact times", "Horas exactas", "Heures précises", "Feste Zeiten", "指定時刻", "زمان‌های دقیق", "Orari esatti", "Horas exatas"),
        row("Time ranges", "Intervalos de tiempo", "Plages horaires", "Zeitfenster", "時間帯", "بازه‌های زمانی", "Intervalli di tempo", "Intervalos de tempo"),
        row("Reminder times", "Horas de recordatorio", "Heures de rappel", "Erinnerungszeiten", "リマインダー時刻", "زمان‌های یادآور", "Orari dei promemoria", "Horas dos lembretes"),
        row("Specific moments during the day", "Momentos específicos del día", "Moments précis de la journée", "Bestimmte Zeitpunkte am Tag", "1日の特定の時刻", "زمان‌های مشخص در روز", "Momenti specifici della giornata", "Momentos específicos do dia"),
        row("No reminder times", "Sin horas de recordatorio", "Aucune heure de rappel", "Keine Erinnerungszeiten", "リマインダー時刻なし", "زمان یادآوری وجود ندارد", "Nessun orario", "Sem horas de lembrete"),
        row("Add a time to receive a word reminder.", "Añade una hora para recibir una palabra.", "Ajoutez une heure pour recevoir un mot.", "Füge eine Zeit für eine Worterinnerung hinzu.", "単語リマインダーを受け取る時刻を追加してください。", "زمانی برای دریافت یادآور واژه اضافه کنید.", "Aggiungi un orario per ricevere una parola.", "Adicione uma hora para receber uma palavra."),
        row("Reminder windows", "Ventanas de recordatorio", "Fenêtres de rappel", "Erinnerungsfenster", "リマインダー時間帯", "بازه‌های یادآور", "Finestre dei promemoria", "Janelas de lembrete"),
        row("{0} reminder windows", "{0} ventanas de recordatorio", "{0} fenêtres de rappel", "{0} Erinnerungsfenster", "{0}個のリマインダー時間帯", "{0} بازه یادآور", "{0} finestre dei promemoria", "{0} janelas de lembrete"),
        row("{0}–{1}, every {2} min", "{0}–{1}, cada {2} min", "{0}–{1}, toutes les {2} min", "{0}–{1}, alle {2} Min.", "{0}～{1}、{2}分ごと", "{0} تا {1}، هر {2} دقیقه", "{0}–{1}, ogni {2} min", "{0}–{1}, a cada {2} min"),
        row("Repeated reminders inside time ranges", "Recordatorios repetidos en intervalos", "Rappels répétés dans des plages horaires", "Wiederholte Erinnerungen in Zeitfenstern", "時間帯内で繰り返すリマインダー", "یادآورهای تکراری در بازه‌های زمانی", "Promemoria ripetuti negli intervalli", "Lembretes repetidos em intervalos"),
        row("No time windows", "Sin ventanas de tiempo", "Aucune plage horaire", "Keine Zeitfenster", "時間帯なし", "بازه زمانی وجود ندارد", "Nessun intervallo", "Sem janelas de tempo"),
        row("Add a window to repeat reminders during a part of the day.", "Añade un intervalo para repetir recordatorios.", "Ajoutez une plage pour répéter les rappels.", "Füge ein Fenster für wiederholte Erinnerungen hinzu.", "時間帯を追加してリマインダーを繰り返してください。", "بازه‌ای برای تکرار یادآورها اضافه کنید.", "Aggiungi un intervallo per ripetere i promemoria.", "Adicione uma janela para repetir lembretes."),
        row("Reminder {0}", "Recordatorio {0}", "Rappel {0}", "Erinnerung {0}", "リマインダー {0}", "یادآور {0}", "Promemoria {0}", "Lembrete {0}"),
        row("Window {0}", "Ventana {0}", "Fenêtre {0}", "Fenster {0}", "時間帯 {0}", "بازه {0}", "Intervallo {0}", "Janela {0}"),
        row("Hour", "Hora", "Heure", "Stunde", "時", "ساعت", "Ora", "Hora"),
        row("Minute", "Minuto", "Minute", "Minute", "分", "دقیقه", "Minuto", "Minuto"),
        row("Start hour", "Hora de inicio", "Heure de début", "Startstunde", "開始時", "ساعت شروع", "Ora iniziale", "Hora inicial"),
        row("End hour", "Hora de fin", "Heure de fin", "Endstunde", "終了時", "ساعت پایان", "Ora finale", "Hora final"),
        row("Every (min)", "Cada (min)", "Toutes les (min)", "Alle (Min.)", "間隔（分）", "هر (دقیقه)", "Ogni (min)", "A cada (min)"),
        row("Decrease {0}", "Disminuir {0}", "Diminuer {0}", "{0} verringern", "{0}を減らす", "کاهش {0}", "Diminuisci {0}", "Diminuir {0}"),
        row("Increase {0}", "Aumentar {0}", "Augmenter {0}", "{0} erhöhen", "{0}を増やす", "افزایش {0}", "Aumenta {0}", "Aumentar {0}"),
        row("Name", "Nombre", "Nom", "Name", "名前", "نام", "Nome", "Nome"),
        row("No words yet. Add your first word.", "Aún no hay palabras. Añade la primera.", "Aucun mot. Ajoutez le premier.", "Noch keine Wörter. Füge dein erstes hinzu.", "単語はまだありません。最初の単語を追加してください。", "هنوز واژه‌ای نیست. نخستین واژه را اضافه کنید.", "Nessuna parola. Aggiungi la prima.", "Ainda não existem palavras. Adicione a primeira."),
        row("Filter category", "Filtrar categoría", "Filtrer la catégorie", "Kategorie filtern", "カテゴリを絞り込む", "فیلتر دسته", "Filtra categoria", "Filtrar categoria"),
        row("Category: {0}", "Categoría: {0}", "Catégorie : {0}", "Kategorie: {0}", "カテゴリ：{0}", "دسته: {0}", "Categoria: {0}", "Categoria: {0}")
    )

    val rowsByKey: Map<String, TranslationRow> = rows.associateBy(TranslationRow::en).also {
        check(it.size == rows.size) { "Duplicate English localization key." }
    }

    fun stringsFor(languageCode: String?): AppStrings =
        AppStrings(languageCode?.takeIf(supportedLanguageCodes::contains) ?: "en")

    private fun row(
        en: String,
        es: String,
        fr: String,
        de: String,
        ja: String,
        fa: String,
        it: String,
        pt: String
    ) = TranslationRow(en, es, fr, de, ja, fa, it, pt)
}

val LocalAppStrings = staticCompositionLocalOf { AppStringCatalog.stringsFor("en") }
