# Hướng dẫn kích hoạt CI/CD cho dự án Auto Novel

## Tổng quan

Dự án này đã có sẵn CI/CD setup với GitHub Actions để tự động build và push Docker images lên GitHub Container Registry (ghcr.io).

## Các bước kích hoạt CI/CD

### 1. Kiểm tra và cập nhật cấu hình (nếu cần)

Workflow files hiện tại đang sử dụng `IMAGE_OWNER: auto-novel`. Nếu bạn muốn push images lên registry của riêng bạn, bạn cần:

- Mở file `.github/workflows/cd-web.yml` và `.github/workflows/cd-api.yml`
- Thay đổi `IMAGE_OWNER` từ `auto-novel` sang tên GitHub username hoặc organization của bạn
- Ví dụ: nếu username của bạn là `Lain4504`, thay đổi thành `IMAGE_OWNER: Lain4504`

### 2. Đảm bảo repository có quyền Packages

1. Vào GitHub repository của bạn
2. Vào **Settings** → **Actions** → **General**
3. Trong phần **Workflow permissions**, chọn:
   - ✅ **Read and write permissions**
   - ✅ **Allow GitHub Actions to create and approve pull requests** (tùy chọn)

### 3. Kích hoạt GitHub Actions

1. Vào tab **Actions** trên GitHub repository
2. Nếu lần đầu tiên, GitHub sẽ hỏi bạn có muốn enable GitHub Actions không → Chọn **Enable**

### 4. Cách chạy CI/CD

#### Cách 1: Tự động chạy khi push code (Khuyến nghị)

Workflow sẽ tự động chạy khi:
- Push code lên nhánh `refactoring`
- Có thay đổi trong thư mục `web/**` (cho cd-web.yml)
- Có thay đổi trong thư mục `server/**` (cho cd-api.yml)

```bash
# Ví dụ: sau khi thay đổi code
git add .
git commit -m "Update code"
git push origin refactoring
```

#### Cách 2: Chạy thủ công (Manual trigger)

1. Vào tab **Actions** trên GitHub
2. Chọn workflow bạn muốn chạy (Publish Web hoặc Publish Api)
3. Click **Run workflow** → Chọn nhánh → Click **Run workflow**

### 5. Kiểm tra kết quả

1. Vào tab **Actions** để xem trạng thái workflow
2. Click vào workflow run để xem chi tiết
3. Nếu thành công, images sẽ được push lên:
   - `ghcr.io/[IMAGE_OWNER]/auto-novel-web:latest`
   - `ghcr.io/[IMAGE_OWNER]/auto-novel-server:latest`

### 6. Sử dụng images đã build

Sau khi CI/CD chạy thành công, bạn có thể:

1. **Cập nhật docker-compose.yml** để sử dụng images từ registry của bạn:
   ```yaml
   web:
     image: ghcr.io/[YOUR_USERNAME]/auto-novel-web:latest
   
   server:
     image: ghcr.io/[YOUR_USERNAME]/auto-novel-server:latest
   ```

2. **Pull và chạy images**:
   ```bash
   # Đăng nhập vào GitHub Container Registry (nếu images là private)
   echo $GITHUB_TOKEN | docker login ghcr.io -u [YOUR_USERNAME] --password-stdin
   
   # Pull images
   docker pull ghcr.io/[YOUR_USERNAME]/auto-novel-web:latest
   docker pull ghcr.io/[YOUR_USERNAME]/auto-novel-server:latest
   
   # Hoặc dùng docker-compose
   docker compose pull
   docker compose up -d
   ```

## Lưu ý quan trọng

1. **GITHUB_TOKEN**: Token này được GitHub tự động cung cấp, bạn không cần tạo thủ công
2. **Package visibility**: 
   - Nếu repository là public → images sẽ là public
   - Nếu repository là private → images sẽ là private (cần đăng nhập để pull)
3. **Image cleanup**: Workflow tự động xóa các version cũ, chỉ giữ lại 1 version mới nhất
4. **Concurrency**: Nếu có nhiều workflow chạy cùng lúc, chỉ workflow mới nhất sẽ chạy, các workflow cũ sẽ bị hủy

## Troubleshooting

### Workflow không chạy
- Kiểm tra xem GitHub Actions đã được enable chưa
- Kiểm tra xem bạn đã push lên nhánh `refactoring` chưa
- Kiểm tra xem có thay đổi trong `web/**` hoặc `server/**` không

### Build thất bại
- Xem logs trong tab Actions để biết lỗi cụ thể
- Kiểm tra Dockerfile có đúng không
- Kiểm tra quyền Packages đã được cấp chưa

### Không pull được images
- Kiểm tra images có tồn tại trên ghcr.io không
- Nếu images là private, cần đăng nhập với Personal Access Token có quyền `read:packages`
- Kiểm tra tên image có đúng không (username/package-name)

## Tài liệu tham khảo

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

