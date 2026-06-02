http://localhost:8080/ingredients

#ディレクトリ構成
food-management-app/ (プロジェクトのルート)
├── pom.xml                             # プロジェクトの設計図（LombokやJPAの依存関係を記述）
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── foodapp/
        │               ├── FoodApplication.java # アプリを起動するメインクラス
        │               │
        │               ├── config/
        │               │   └── AppConfig.java          # アプリ全体の共通設定（必要に応じて）
        │               │
        │               ├── entity/                   # ★データベースのテーブルに対応
        │               │   ├── Food.java               # id, foodName, amount, taste_limit など
        │               │   └── Category.java           # id, categoryName, unit など
        │               │
        │               ├── repository/               # ★DB操作（SQL自動生成）の担当
        │               │   ├── FoodRepository.java     # JpaRepositoryを継承
        │               │   └── CategoryRepository.java # JpaRepositoryを継承
        │               │
        │               ├── service/                   # ★ビジネスロジック（CRUDの具体的な処理）
        │               │   ├── FoodService.java        # 食品の登録・更新・削除のロジック
        │               │   └── CategoryService.java    # カテゴリ取得などのロジック
        │               │
        │               └── controller/                # ★画面からのリクエストの受付窓口
        │                   ├── FoodController.java     # 食品のCRUD画面の制御
        │                   └── CategoryController.java # カテゴリ選択肢などの制御
        │
        └── resources/
            ├── application.properties                  # ★DB接続設定（ユーザー名やパスワードなど）
            ├── static/                                 # CSSやJavaScript、画像などの静的ファイル
            │   ├── css/
            │   │   └── style.css
            │   └── js/
            │       └── main.js
            └── templates/                              # ★HTML画面（Thymeleafテンプレート）
                ├── layout/
                │   └── base.html                       # ヘッダー・フッターなどの共通枠
                └── foods/
                    ├── index.html                      # 食品一覧画面（R）
                    ├── create.html                     # 食品登録画面（C）
                    ├── edit.html                       # 食品編集画面（U）
                    └── detail.html                     # 食品詳細画面（お好みで）
