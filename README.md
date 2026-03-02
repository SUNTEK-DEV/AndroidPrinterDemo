# PrintDemo - USB Printer & Cash Drawer Controller

A demonstration Android application for controlling USB thermal printers and cash drawers, specifically designed for Point of Sale (POS) systems.

## Overview

This application demonstrates how to integrate a USB thermal printer with cash drawer control functionality into an Android application. It's particularly useful for POS systems, retail environments, and any scenario requiring receipt printing and secure cash storage.

## Features

- **USB Printer Connection**: Automatically detects and connects to compatible USB thermal printers
- **Cash Drawer Control**: Opens cash drawer through printer interface
- **Text Printing**: Prints text with GB2312 character encoding support
- **USB Permission Management**: Handles Android USB device permissions properly
- **Real-time Status Display**: Shows connection status and operation results

## Hardware Requirements

### Supported Printer
- **Model**: USB Thermal Printer (VID: `0x0483`, PID: `0x5720`)
- **Connection**: USB Host mode
- **SDK**: Printer SDK v5.7.3

### Cash Drawer Compatibility
The cash drawer connects to the printer through a standard cash drawer interface (typically an RJ11/RJ12 connector). The printer acts as a controller, sending electrical signals to trigger the cash drawer solenoid.

## How the Cash Drawer Opens

### Technical Mechanism

The cash drawer opening mechanism works through the following process:

#### 1. Hardware Connection
```
Android Device → USB Cable → Thermal Printer → Cash Drawer Cable → Cash Drawer Solenoid
```

The printer has a dedicated cash drawer port that supplies power to the cash drawer's electromagnetic solenoid. When triggered, the solenoid releases the drawer latch, allowing the spring-loaded drawer to open.

#### 2. Software Implementation

In `MainActivity.java` (lines 191-214), the cash drawer is opened using:

```java
private void openCashBox() {
    if (mPrinter == null) {
        showStatus("Printer is not connected");
        return;
    }

    new Thread(() -> {
        try {
            // Send command to printer to activate cash drawer solenoid
            mPrinter.openCashbox(true, false);

            runOnUiThread(() -> {
                showStatus("The cash box is open.");
                Toast.makeText(MainActivity.this, "The cash box is open.", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            runOnUiThread(() ->
                showStatus("Cash drawer failed to open: " + e.getMessage()));
            e.printStackTrace();
        }
    }).start();
}
```

#### 3. Printer SDK Method

The `openCashbox(boolean, boolean)` method sends specific ESC/POS commands to the printer:

- **First parameter (true)**: Activates the primary cash drawer kick-out pulse
- **Second parameter (false)**: Deactivates the secondary/backup cash drawer pulse (if available)

This command instructs the printer to send a momentary electrical pulse (typically 24V) to the cash drawer's solenoid for approximately 200-300ms, which is sufficient to release the latch mechanism.

#### 4. Signal Flow

```
App → Printer SDK → USB Protocol → Printer Controller → Cash Drawer Circuit → Solenoid Pulse → Drawer Opens
```

The printer generates the electrical pulse through its internal cash drawer drive circuit, which:
1. Receives the digital command from the Android device via USB
2. Converts it to a high-voltage pulse (typically 24V DC)
3. Delivers the pulse to the cash drawer connector
4. The energized solenoid pulls back the latch bolt
5. The spring-loaded drawer pushes open

## Installation & Setup

### 1. Build the Application

```bash
./gradlew assembleDebug
```

### 2. Install on Android Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Configure Printer Device ID

If your printer has different Vendor ID (VID) or Product ID (PID), update `MainActivity.java` (lines 184-185):

```java
final int PRINTER_VENDOR_ID = 0x0483;  // Change to your printer's VID
final int PRINTER_PRODUCT_ID = 0x5720; // Change to your printer's PID
```

To find your printer's VID/PID on Windows:
- Open Device Manager
- Find your printer under "Universal Serial Bus devices"
- Right-click → Properties → Details → Hardware Ids

## Usage

### Step 1: Connect Printer
1. Connect the USB printer to your Android device via USB OTG (On-The-Go) cable
2. Launch the PrintDemo app
3. Tap the **"连接打印机" (Connect Printer)** button
4. Grant USB permission when prompted
5. Wait for "打印机连接成功" (Printer connected successfully) status

### Step 2: Open Cash Drawer
1. Ensure printer is connected (the button will be enabled)
2. Tap the **"打开钱箱" (Open Cash Box)** button
3. The printer will send the pulse to the cash drawer
4. Cash drawer should open immediately

### Step 3: Print Text
1. Tap the **"打印文字" (Print Text)** button
2. The printer will print a test line with GB2312 encoding

## Technical Details

### Architecture

- **Language**: Java
- **Minimum SDK**: Android 11 (API 30)
- **Target SDK**: Android 14 (API 36)
- **Printer SDK**: Printer SDK v5.7.3 (located in `app/libs/`)

### Key Components

#### MainActivity.java
- **USB Connection Management**: Handles device discovery, permission requests, and connection establishment
- **Cash Drawer Control**: Implements `openCashbox()` method
- **Printing Function**: Implements text printing with proper character encoding
- **Broadcast Receiver**: Listens for USB permission results

#### Connection Handler (lines 150-179)
Uses a `Handler` with message codes to manage connection states:
- `101`, `102`, `103`: Connection successful
- Enables cash drawer and print buttons upon successful connection

#### USB Permission Handling (lines 220-241)
Implements `BroadcastReceiver` to handle USB permission grant results:
- Automatically connects printer after permission is granted
- Updates UI status accordingly

### Permissions Required

The app requires the following permissions (defined in `AndroidManifest.xml`):
- `android.hardware.usb.host` - USB device communication
- Various standard Android permissions for hardware access

## Troubleshooting

### Printer Not Detected
- Ensure USB OTG is supported on your device
- Check USB cable connections
- Verify VID/PID match your printer's hardware IDs
- Check the logcat for USB device detection logs

### Cash Drawer Not Opening
1. **Check Physical Connections**:
   - Ensure cash drawer cable is securely plugged into printer's cash drawer port
   - Verify cable is not damaged

2. **Verify Printer Connection**:
   - Printer must be connected before cash drawer will open
   - Check app status shows "打印机连接成功"

3. **Test Solenoid**:
   - Manual test: Use printer's self-test function (if available)
   - Check if solenoid clicks when activated

4. **Common Issues**:
   - **Weak Battery**: If printer is battery-powered, ensure sufficient charge
   - **Wrong Cable**: Use the correct cash drawer interface cable (RJ11/RJ12)
   - **Solenoid Failure**: Cash drawer solenoid may be faulty or disconnected
   - **Firmware Issue**: Some printers require firmware configuration for cash drawer control

### USB Permission Denied
- Go to Android Settings → Apps → PrintDemo → Permissions
- Ensure USB device permissions are granted
- Try unplugging and replugging the USB cable

## Development

### Project Structure

```
printdemo/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/suntek/printdemo/
│   │       │   └── MainActivity.java          # Main activity with printer control
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml       # UI layout
│   │       │   └── xml/
│   │       │       └── device_filter.xml       # USB device filter
│   │       └── AndroidManifest.xml
│   └── libs/
│       └── printersdkv5.7.3.jar               # Printer SDK
├── build.gradle
└── README.md
```

### Dependencies

- AndroidX AppCompat
- Material Design Components
- Printer SDK v5.7.3 (JAR)

### Customization

#### Modify Cash Drawer Pulse Duration
Some printer SDKs allow customization of the pulse duration. If needed, explore alternative methods:
```java
// Example: Different pulse configurations
mPrinter.openCashbox(true, false);  // Standard pulse
// mPrinter.openCashBoxTSPL(1, 0);   // Alternative TSPL command
```

#### Add Multiple Cash Drawers
For systems with two cash drawers:
```java
mPrinter.openCashbox(true, true);  // Activate both primary and secondary
```

## Safety Notes

⚠️ **Important Safety Information**:
- The cash drawer solenoid operates at higher voltages (typically 12-24V DC)
- Always ensure proper cable connections before testing
- Disconnect power before making any hardware changes
- Keep fingers clear of cash drawer during operation (it opens with spring force)

## License

This is a demonstration project for educational purposes.

## Support

For issues related to:
- **Printer Hardware**: Contact your printer manufacturer
- **Cash Drawer**: Contact cash drawer supplier
- **Android App Integration**: Refer to the Printer SDK documentation

## Version History

- **v1.0** - Initial release
  - USB printer connection
  - Cash drawer control
  - Basic text printing

---

**Note**: This application is specifically designed for use with compatible USB thermal printers. Always verify hardware compatibility before deployment in production environments.
