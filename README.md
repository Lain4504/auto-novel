# Robot dịch máy Light Novel

[![GPL-3.0](https://img.shields.io/github/license/auto-novel/auto-novel)](https://github.com/auto-novel/auto-novel#license)
[![cd-web](https://github.com/auto-novel/auto-novel/actions/workflows/cd-web.yml/badge.svg)](https://github.com/auto-novel/auto-novel/actions/workflows/cd-web.yml)
[![cd-api](https://github.com/auto-novel/auto-novel/actions/workflows/cd-api.yml/badge.svg)](https://github.com/auto-novel/auto-novel/actions/workflows/cd-api.yml)

> Xây lại Tháp Babel!!

[Robot dịch máy Light Novel](https://books.fishhawk.top/) là một trang web tự động tạo bản dịch máy cho light novel và chia sẻ chúng. Tại đây bạn có thể duyệt các web novel/light novel tiếng Nhật, hoặc tải lên EPUB/TXT của riêng mình rồi tạo bản dịch máy.

## Tính năng

- Duyệt novel Nhật với các nguồn: [Kakuyomu](https://kakuyomu.jp/), [Syosetu](https://syosetu.com/), [Novelup](https://novelup.plus/), [Hameln](https://syosetu.org/), [Pixiv](https://www.pixiv.net/), [Alphapolis](https://www.alphapolis.co.jp/).
- Tạo bản dịch máy bằng nhiều dịch vụ: Baidu, Youdao, API kiểu OpenAI (ví dụ DeepSeek API), [Sakura](https://huggingface.co/SakuraLLM/Sakura-14B-Qwen2.5-v1.0-GGUF).
- Hỗ trợ bảng thuật ngữ.
- Hỗ trợ nhiều định dạng: nguyên gốc tiếng Nhật, tiếng Trung hoặc song song Nhật-Trung.
- Sinh file EPUB và TXT.
- Dịch file EPUB và TXT.
- Đọc trực tuyến ngay trên web.

## Đóng góp

Vui lòng xem [CONTRIBUTING.md](https://github.com/auto-novel/auto-novel/blob/main/CONTRIBUTING.md)

<a href="https://next.ossinsight.io/widgets/official/compose-recent-top-contributors?repo_id=559577341" target="_blank" style="display: block" align="left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://next.ossinsight.io/widgets/official/compose-recent-top-contributors/thumbnail.png?repo_id=559577341&image_size=auto&color_scheme=dark" width="280">
    <img alt="Top Contributors of ant-design/ant-design - Last 28 days" src="https://next.ossinsight.io/widgets/official/compose-recent-top-contributors/thumbnail.png?repo_id=559577341&image_size=auto&color_scheme=light" width="280">
  </picture>
</a>

## Triển khai

> [!WARNING]
> Lưu ý: dự án này không được thiết kế cho việc tự triển khai, không đảm bảo mọi tính năng hoạt động hoặc tương thích về sau.

Tải mã nguồn:

```bash
> git clone https://github.com/auto-novel/auto-novel.git
> cd auto-novel
```

Tạo rồi chỉnh sửa tệp `.env` với nội dung:

```bash
DATA_PATH=./data                      # thư mục lưu trữ dữ liệu
HTTPS_PROXY=https://127.0.0.1:7890    # proxy cho web novel, có thể để trống
PIXIV_COOKIE_PHPSESSID=               # cookie Pixiv, bỏ trống nếu không dùng Pixiv
```

Mở `docker-compose.yml` và chỉnh sửa nếu cần.

Chạy `docker compose up [-d]` (`-d` để chạy nền).

Truy cập `http://localhost` để sử dụng.
