import { UAParser } from 'ua-parser-js';

export const parseDeviceName = (userAgentString) => {
  if (!userAgentString) return "Thiết bị không xác định";

  const parser = new UAParser(userAgentString);
  const result = parser.getResult();

  // Bóc tách dữ liệu
  const osName = result.os.name || '';
  const osVersion = result.os.version || '';
  const deviceVendor = result.device.vendor || ''; // Apple, Samsung...
  const deviceModel = result.device.model || '';   // iPhone, Galaxy...
  const browserName = result.browser.name || '';

  // Định dạng chuỗi hiển thị
  const osFull = `${osName} ${osVersion}`.trim();
  const deviceFull = `${deviceVendor} ${deviceModel}`.trim();

  // Logic ưu tiên hiển thị:
  // 1. Ưu tiên hiện Tên máy + Hệ điều hành (VD: Apple iPhone (iOS 16.6))
  if (deviceFull && osFull) {
    return `${deviceFull} (${osFull})`;
  }
  
  // 2. Nếu quét bằng Web PC không có tên máy, hiện Hệ điều hành + Trình duyệt (VD: Windows 11 - Chrome)
  if (osFull && browserName) {
    return `${osFull} - ${browserName}`;
  }

  // 3. Fallback mặc định
  return deviceFull || osFull || browserName || "Thiết bị không xác định";
};