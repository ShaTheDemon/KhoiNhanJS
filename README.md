# KhoinhanJS

Addon KubeJS cho máy chủ Khối Nhân: gọi HTTP (`fetch`), điều khiển Discord bot và các tiện ích server ngay trong script KubeJS.

## Tính năng

- **HTTP/Fetch** – gửi request từ script KubeJS
- **Discord bot** – gửi tin nhắn, embed, lắng nghe tin nhắn Discord
- **Tiện ích server** – gửi tin nhắn có mã màu cho người chơi, lấy thông tin server
- **Hệ thống an toàn** – tự tắt tính năng mạng khi chơi singleplayer

## Cài đặt

1. Build (`./gradlew build`) hoặc lấy file jar từ release của bạn
2. Bỏ jar vào thư mục `mods`
3. Cần có [KubeJS](https://kubejs.com/)
4. Khởi động lại server

## Hệ thống an toàn (registry)

- Singleplayer: toàn bộ tính năng mạng bị tắt mặc định và có cảnh báo trong chat.
- Dedicated server: tự bật.

Lệnh (yêu cầu OP cấp 2):

```
/khoinhanjs enable    # bật registry
/khoinhanjs disable   # tắt registry
/khoinhanjs reload    # yêu cầu nạp lại bindings (khi registry đang bật)
/khoinhanjs status    # xem trạng thái
```

Sau khi bật, chạy `/kubejs reload server` để script nạp lại với bindings mới.

## API

Các binding sau có sẵn toàn cục trong script KubeJS (khi registry bật):

| Tên | Loại | Mô tả |
|-----|------|-------|
| `fetch()` | Hàm | HTTP request |
| `fetchAsync()` | Hàm | HTTP request bất đồng bộ |
| `DiscordBot` | Class | Discord bot |
| `Server` | Class | Tiện ích server |
| `FetchBinding` | Class | Tiện ích HTTP |
| `FetchOptions` | Class | Tuỳ chọn request |
| `FetchResponse` | Class | Kết quả response |

### fetch

```javascript
fetch('https://api.example.com/data')

fetchAsync('https://api.example.com/data', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ key: 'value' })
})
```

### DiscordBot

```javascript
const bot = new DiscordBot({
    token: "bot-token",
    guild: "guild-id",
    channels: { chat: "channel-id", announcements: "channel-id" },
    sanitizeMessages: true
});

bot.sendMessage("chat", "Xin chào từ Minecraft!");

bot.sendEmbed("announcements", {
    title: "Trạng thái server",
    description: "Server đang online!",
    color: 0x00ff00
});

bot.onMessage(function (msg) {
    console.log(msg.getAuthor() + ": " + msg.getContent());
});

bot.setActivity("Minecraft Server Online");
```

### Server

```javascript
Server.sendRawMessage("&aChào mừng đến server!");
Server.sendRawMessageToPlayer("TenNguoiChoi", "&bXin chào!");
const count = Server.getPlayerCount();
const names = Server.getPlayerNames();
```

## Yêu cầu

- Minecraft 1.21.1
- NeoForge 21.1.200+
- KubeJS 2101.7.1+

## Build

```bash
./gradlew build
```

Jar nằm ở `build/libs/khoinhanjs-1.21.1-{version}.jar`.

## Giấy phép

MIT – xem [LICENSE](LICENSE).
