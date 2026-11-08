# 🔄 API Routes Mapping - Old → New

## 📋 Tổng quan thay đổi

Backend đã được tái cấu trúc theo mô hình **Public / User / Admin**:
- 🌐 **`/api/public/*`** - Endpoints công khai (không cần authentication)
- 👤 **`/api/user/*`** - Endpoints cho user đã đăng nhập
- 🔐 **`/api/admin/*`** - Endpoints chỉ dành cho admin

---

## 🌐 PUBLIC ENDPOINTS (Không cần authentication)

### Categories
| Old Route | New Route | Method | Description |
|-----------|-----------|--------|-------------|
| `/api/categories` | `/api/public/categories` | GET | Lấy danh sách thể loại |

### Stories
| Old Route | New Route | Method | Description |
|-----------|-----------|--------|-------------|
| `/api/stories` | `/api/public/stories` | GET | Lấy danh sách truyện công khai |
| `/api/stories/latest` | `/api/public/stories/latest` | GET | Lấy truyện mới nhất |
| `/api/stories/category/{categoryId}` | `/api/public/stories/category/{categoryId}` | GET | Lấy truyện theo thể loại |

### Chapters
| Old Route | New Route | Method | Description |
|-----------|-----------|--------|-------------|
| `/api/chapters/story/{storyId}` | `/api/public/chapters/story/{storyId}` | GET | Lấy chapters của truyện |

### Payment (VNPay callbacks)
| Old Route | New Route | Method | Description |
|-----------|-----------|--------|-------------|
| `/api/payment/vnpay/return` | `/api/public/payment/vnpay/return` | GET | VNPay return callback |
| `/api/payment/vnpay/ipn` | `/api/public/payment/vnpay/ipn` | GET | VNPay IPN callback |

---

## 🔐 AUTH ENDPOINTS (Giữ nguyên)

| Route | Method | Description |
|-------|--------|-------------|
| `/api/auth/register` | POST | Đăng ký tài khoản |
| `/api/auth/login` | POST | Đăng nhập |
| `/api/auth/me` | GET | Lấy thông tin user hiện tại |
| `/api/auth/profile` | PUT | Cập nhật profile |
| `/api/auth/change-password` | PUT | Đổi mật khẩu |

---

## 👤 USER ENDPOINTS (Cần authentication)

### Orders & Purchases
| Old Route | New Route | Method | Description |
|-----------|-----------|--------|-------------|
| `/api/orders/purchase-story` | `/api/user/orders/purchase-story` | POST | Mua truyện |
| `/api/orders/my-purchases` | `/api/user/orders/my-purchases` | GET | Lấy danh sách truyện đã mua |

### Story Ownership
| Old Route | New Route | Method | Description |
|-----------|-----------|--------|-------------|
| `/api/story-ownerships/check/{storyId}` | `/api/user/story-ownerships/check/{storyId}` | GET | Kiểm tra ownership |

### Payment
| Old Route | New Route | Method | Description |
|-----------|-----------|--------|-------------|
| `/api/payment/vnpay/create-payment` | `/api/user/payment/vnpay/create-payment` | POST | Tạo payment URL (nạp tiền) |

---

## 🔐 ADMIN ENDPOINTS (Giữ nguyên prefix)

### Admin Stats & Payments
| Route | Method | Description |
|-------|--------|-------------|
| `/api/admin/statistics` | GET | Lấy thống kê admin |
| `/api/admin/payments` | GET | Lấy lịch sử thanh toán |

### Stories Management
| Route | Method | Description |
|-------|--------|-------------|
| `/api/admin/stories` | GET | Lấy tất cả truyện |
| `/api/admin/stories/{id}` | GET | Lấy chi tiết truyện |
| `/api/admin/stories` | POST | Tạo truyện mới |
| `/api/admin/stories/{id}` | PUT | Cập nhật truyện |
| `/api/admin/stories/{id}` | DELETE | Xóa truyện |
| `/api/admin/stories/category/{categoryId}` | GET | Lấy truyện theo category |
| `/api/admin/stories/status/{status}` | GET | Lấy truyện theo status |
| `/api/admin/stories/upload-cover` | POST | Upload ảnh bìa truyện |

### Chapters Management
| Route | Method | Description |
|-------|--------|-------------|
| `/api/admin/chapters` | GET | Lấy tất cả chapters |
| `/api/admin/chapters/story/{storyId}` | GET | Lấy chapters theo truyện |
| `/api/admin/chapters/{id}` | GET | Lấy chi tiết chapter |
| `/api/admin/chapters` | POST | Tạo chapter mới |
| `/api/admin/chapters/{id}` | PUT | Cập nhật chapter |
| `/api/admin/chapters/{id}` | DELETE | Xóa chapter |
| `/api/admin/chapters/upload-audio` | POST | Upload file audio |

---

## 🚨 BREAKING CHANGES - Cần update Frontend

### ❌ Routes đã thay đổi (404 nếu dùng route cũ):

1. **Categories:**
   ```diff
   - GET /api/categories
   + GET /api/public/categories
   ```

2. **Public Stories:**
   ```diff
   - GET /api/stories
   + GET /api/public/stories
   
   - GET /api/stories/latest
   + GET /api/public/stories/latest
   
   - GET /api/stories/category/{categoryId}
   + GET /api/public/stories/category/{categoryId}
   ```

3. **Public Chapters:**
   ```diff
   - GET /api/chapters/story/{storyId}
   + GET /api/public/chapters/story/{storyId}
   ```

4. **User Orders:**
   ```diff
   - POST /api/orders/purchase-story
   + POST /api/user/orders/purchase-story
   
   - GET /api/orders/my-purchases
   + GET /api/user/orders/my-purchases
   ```

5. **User Ownership:**
   ```diff
   - GET /api/story-ownerships/check/{storyId}
   + GET /api/user/story-ownerships/check/{storyId}
   ```

6. **User Payment:**
   ```diff
   - POST /api/payment/vnpay/create-payment
   + POST /api/user/payment/vnpay/create-payment
   ```

7. **Public Payment Callbacks:**
   ```diff
   - GET /api/payment/vnpay/return
   + GET /api/public/payment/vnpay/return
   
   - GET /api/payment/vnpay/ipn
   + GET /api/public/payment/vnpay/ipn
   ```

---

## 📝 Cách update Frontend

### 1. Update file `constants.js`:

```javascript
// Public endpoints
export const API_ENDPOINTS = {
  // Auth (giữ nguyên)
  AUTH: {
    REGISTER: '/auth/register',
    LOGIN: '/auth/login',
    ME: '/auth/me',
    UPDATE_PROFILE: '/auth/profile',
    CHANGE_PASSWORD: '/auth/change-password'
  },

  // Public
  PUBLIC: {
    CATEGORIES: '/public/categories',
    STORIES: '/public/stories',
    STORIES_LATEST: '/public/stories/latest',
    STORIES_BY_CATEGORY: (categoryId) => `/public/stories/category/${categoryId}`,
    CHAPTERS_BY_STORY: (storyId) => `/public/chapters/story/${storyId}`,
    PAYMENT_RETURN: '/public/payment/vnpay/return',
    PAYMENT_IPN: '/public/payment/vnpay/ipn'
  },

  // User
  USER: {
    PURCHASE_STORY: '/user/orders/purchase-story',
    MY_PURCHASES: '/user/orders/my-purchases',
    CHECK_OWNERSHIP: (storyId) => `/user/story-ownerships/check/${storyId}`,
    CREATE_PAYMENT: '/user/payment/vnpay/create-payment'
  },

  // Admin (giữ nguyên)
  ADMIN: {
    STATISTICS: '/admin/statistics',
    PAYMENTS: '/admin/payments',
    STORIES: '/admin/stories',
    CHAPTERS: '/admin/chapters'
  }
};
```

### 2. Update các API calls trong components:

```javascript
// OLD
const categories = await api.get('/api/categories');

// NEW
const categories = await api.get('/api/public/categories');
```

### 3. Tìm và thay thế toàn bộ project:

**Find:** `/api/categories`  
**Replace:** `/api/public/categories`

**Find:** `/api/stories`  
**Replace:** `/api/public/stories`

**Find:** `/api/chapters/story`  
**Replace:** `/api/public/chapters/story`

**Find:** `/api/orders`  
**Replace:** `/api/user/orders`

**Find:** `/api/story-ownerships`  
**Replace:** `/api/user/story-ownerships`

---

## ✅ Các routes KHÔNG đổi

- ✅ `/api/auth/*` - Authentication endpoints
- ✅ `/api/admin/*` - Admin endpoints (stories, chapters, statistics, payments)

---

## 🔍 Quick Fix - Frontend Update Checklist

- [ ] Update `constants.js` với endpoints mới
- [ ] Update `api.js` functions
- [ ] Tìm và thay `/api/categories` → `/api/public/categories`
- [ ] Tìm và thay `/api/stories` → `/api/public/stories`
- [ ] Tìm và thay `/api/chapters/story` → `/api/public/chapters/story`
- [ ] Tìm và thay `/api/orders` → `/api/user/orders`
- [ ] Tìm và thay `/api/story-ownerships` → `/api/user/story-ownerships`
- [ ] Tìm và thay `/api/payment/vnpay/create-payment` → `/api/user/payment/vnpay/create-payment`
- [ ] Test tất cả pages: Home, Stories, Admin
- [ ] Check browser console không có 404 errors

---

## 📞 Support

Nếu gặp lỗi 404:
1. Kiểm tra route có prefix `/api/public`, `/api/user`, hoặc `/api/admin`
2. Xem file này để tìm route mapping
3. Update frontend theo hướng dẫn trên
