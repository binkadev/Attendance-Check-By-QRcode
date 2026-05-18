import { UAParser } from 'ua-parser-js';

export const parseDeviceName = (userAgentString) => {
  if (!userAgentString) return "Thiết bị không xác định";

  const uaLower = userAgentString.toLowerCase();
  if (
    uaLower === 'unknown' || 
    uaLower === 'undefined' || 
    uaLower === 'null' || 
    uaLower.includes('không xác định') ||
    uaLower.includes('không rõ') ||
    !userAgentString.trim()
  ) {
    return "Thiết bị không xác định";
  }

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

  // Nếu không đọc được cả nhà sản xuất (vendor) và dòng máy (model)
  if (!deviceVendor && !deviceModel) {
    return "Thiết bị không xác định";
  }

  // Logic ưu tiên hiển thị:
  // 1. Ưu tiên hiện Tên máy + Hệ điều hành (VD: Apple iPhone (iOS 16.6))
  if (deviceFull && osFull) {
    return `${deviceFull} (${osFull})`;
  }
  
  if (deviceFull) {
    return deviceFull;
  }

  // 3. Fallback mặc định
  return "Thiết bị không xác định";
};