# Đóng góp mã nguồn

Cảm ơn bạn đã quan tâm đóng góp cho dự án! Để hợp tác hiệu quả, hãy tuân theo các quy tắc sau.

- Trước khi viết code, hãy mô tả kế hoạch thay đổi qua Issue hoặc nhóm thảo luận để bảo đảm thống nhất với định hướng hiện tại.
- Khi gửi Pull Request, giữ nội dung gọn gàng, tập trung vào một thay đổi độc lập để tiện review và merge.
- Nếu thắc mắc về thiết kế hiện tại, cứ @FishHawk trong nhóm.
- Dù có dùng AI hỗ trợ, bạn vẫn phải tự mình rà soát lại.

## Cách tham gia phát triển front-end

Website được xây dựng bằng Vue3 + TypeScript + Vite + [Naive UI](https://www.naiveui.com/zh-CN).

Chuẩn bị môi trường:

```bash
git clone git@github.com:auto-novel/auto-novel.git
cd web
pnpm install --frozen-lockfile # cài phụ thuộc
pnpm prepare                   # thiết lập Git hook
```

Sau đó chọn cách chạy dev server phù hợp:

```bash
pnpm dev        # dev server nối tới backend production của site dịch máy
pnpm dev:local  # dev server nối tới backend chạy local, http://localhost:8081
pnpm dev --host # như pnpm dev nhưng mở LAN, tiện debug trên điện thoại
```

Lưu ý: nếu dev server nối tới backend **production** của site dịch máy, hãy tránh làm bẩn dữ liệu thật. Vì lý do an toàn, môi trường dev đã chặn upload bản dịch chương.

## Cách tham gia phát triển back-end

Backend dùng JVM17 + Kotlin + Ktor, nên mở dự án bằng IntelliJ IDEA.

Nếu thay đổi liên quan tới DB, bạn cần tự [triển khai DB](https://github.com/auto-novel/auto-novel/blob/main/README.md#部署) và đặt biến môi trường:

```bash
DB_HOST_TEST=127.0.0.1 # địa chỉ IP của DB
```

Nếu thay đổi không đụng đến Http API, hãy dùng kotest viết unit test để debug; nên cài plugin kotest.

Nếu cần thao tác Http API, chạy `pnpm dev:local` theo hướng dẫn phần front-end.

> [!NOTE]
> Hướng dẫn cấu hình môi trường NixOS xem tại [flake.nix](https://gist.github.com/kurikomoe/9dd60f9613e0b8f75c137779d223da4f). Vì dùng devenv nên cần `--impure`.
