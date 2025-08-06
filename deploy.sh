#!/bin/bash

# Gallery + BitChat Wireless Deployment Script
# Target: 192.168.0.238:5555

set -e

echo "🚀 Starting deployment to wireless Android device..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check device connection
echo -e "${BLUE}📱 Checking device connection...${NC}"
if adb devices | grep -q "192.168.0.238:5555.*device"; then
    echo -e "${GREEN}✅ Device connected: 192.168.0.238:5555${NC}"
else
    echo -e "${RED}❌ Device not connected. Attempting to connect...${NC}"
    adb connect 192.168.0.238:5555
    sleep 2
    if ! adb devices | grep -q "192.168.0.238:5555.*device"; then
        echo -e "${RED}❌ Failed to connect to device${NC}"
        exit 1
    fi
fi

# Get device info
echo -e "${BLUE}📋 Device Information:${NC}"
adb -s 192.168.0.238:5555 shell getprop ro.product.model
adb -s 192.168.0.238:5555 shell getprop ro.build.version.release

# Build the APK with optimizations
echo -e "${BLUE}🔨 Building APK (this may take a few minutes)...${NC}"
echo -e "${YELLOW}💡 The app has 200+ source files and many dependencies, so this will take time${NC}"

# Use Gradle with optimizations
export GRADLE_OPTS="-Xmx4g -XX:+UseParallelGC"
./gradlew assembleDebug \
    --parallel \
    --build-cache \
    --configure-on-demand \
    --max-workers=4 \
    -Dkotlin.compiler.execution.strategy=in-process \
    -Dkotlin.incremental=false

# Check if APK was built
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo -e "${GREEN}✅ APK built successfully: $APK_PATH${NC}"
    
    # Get APK size
    APK_SIZE=$(stat -f%z "$APK_PATH" 2>/dev/null || stat -c%s "$APK_PATH" 2>/dev/null || echo "unknown")
    echo -e "${BLUE}📦 APK Size: $APK_SIZE bytes${NC}"
else
    echo -e "${RED}❌ APK build failed - file not found: $APK_PATH${NC}"
    exit 1
fi

# Uninstall previous version if exists
echo -e "${BLUE}🗑️  Uninstalling previous version...${NC}"
adb -s 192.168.0.238:5555 uninstall com.google.aiedge.gallery 2>/dev/null || echo "No previous version found"

# Install the APK
echo -e "${BLUE}📲 Installing APK to device...${NC}"
if adb -s 192.168.0.238:5555 install "$APK_PATH"; then
    echo -e "${GREEN}✅ APK installed successfully!${NC}"
else
    echo -e "${RED}❌ APK installation failed${NC}"
    exit 1
fi

# Launch the app
echo -e "${BLUE}🚀 Launching the integrated Gallery + BitChat app...${NC}"
adb -s 192.168.0.238:5555 shell am start -n com.google.aiedge.gallery/com.google.ai.edge.gallery.IntegratedMainActivity

echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
echo -e "${BLUE}📱 The app should now be running on your device${NC}"
echo -e "${YELLOW}💡 First launch will require permissions for Bluetooth, Location, and Camera${NC}"
echo -e "${YELLOW}💡 Gallery features require downloading AI models (2-4GB)${NC}"
echo -e "${YELLOW}💡 BitChat features work immediately for mesh networking${NC}"

# Show final status
echo -e "${BLUE}📊 Final Status:${NC}"
echo -e "  • App Package: com.google.aiedge.gallery"
echo -e "  • Main Activity: IntegratedMainActivity"
echo -e "  • Features: Gallery AI + BitChat Mesh"
echo -e "  • Device: 192.168.0.238:5555"
echo -e "${GREEN}🚀 Ready to use!${NC}"