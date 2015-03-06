Bamboo ChatWork Plugin
=========

このプラグインは Atlassian Bamboo の通知プラグインです。
ビルド結果を ChatWork のルームに通知します。

## インストール

jar ファイルを取得し、Bamboo の Manage add-ons からアップロードします。
あとは標準の HipChat 通知プラグインと同様です。
ビルドプランのNotificationsタブから通知を追加して`ChatWork`が選択可能になっています。

以下の設定値を入力して保存するとビルド結果を指定した room に POST します。

- APIトークン
- room ID
- 通知メッセージかどうか（IM形式かHTML形式か）

本プラグインは ChatWork API v1 を使っています。
APIの仕様変更により、通知が利用できなくなる場合があります。
