BlackBox is a virtual engine, it can clone and run virtual application on Android, users don't have to install APK file to run the application on devices. BlackBox control all virtual applications, so you can do anything you want by using BlackBox.

## For More Information and help

[Telegram](https://t.me/newblackboxa)

## Support
Currently it support android version 5.0 ～ 14.0.

If conditions permit, downgrade targetSdkVersion to 28 or below for better compatibility.

***Stability has not been tested extensively and is for learning and communication purposes only. Please do not use for other purposes***


## Usage
### Step 1.Add initialized code in the Application
```java
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BlackBoxCore.get().doCreate();
    }
```

### Step 2.Install application
```java
    // Use package name to install provided that application has been install on device
    BlackBoxCore.get().installPackageAsUser("com.tencent.mm", userId);
    
    // Use APK path to install provided that application has not been install on device
    BlackBoxCore.get().installPackageAsUser(new File("/sdcard/com.tencent.mm.apk"), userId);
```

### Step 3.Launch application
```java
   BlackBoxCore.get().launchApk("com.tencent.mm", userId);
```

### App Cloner
<img src="assets/multiw.gif" width="50%">

### API
#### Get application list that were installed in BlackBox
```java
   // flags can refer to the Android develop documentation
   BlackBoxCore.get().getInstalledApplications(flags, userId);
   
   BlackBoxCore.get().getInstalledPackages(flags, userId);
```

#### Get user information in BlackBox
```java
   List<BUserInfo> users = BlackBoxCore.get().getUsers();
```
If you want to perform more operations, please refer to the source code.


## 🤝 Contributing

Embark on the journey of contributing to SketchIDE and be a part of shaping the future of non-coding Android app development.

1. **Fork:** Begin by forking this repository.
2. **Make Changes:** Implement your enhancements in your forked repository.
3. **Test:** Ensure your changes work flawlessly.
4. **Pull Request:** Submit a pull request in this repository.
5. **Review & Merge:** Your contribution will be reviewed and merged if accepted by the repository members.


### Commit Message Guidelines

When making changes, please adhere to the following commit message guidelines:

- Use only English for the commit
- Keep the commit message short and detailed.
- Use one of these commit types as a prefix:
    - `feat:` for a feature, possibly improving something already existing.
    - `fix:` for a bug fix.
    - `design:` for UI/UX improvements.
    - `refactor:` for refactoring a specific section of the codebase.
    - `test:` for everything related to testing.
    - `docs:` for documentation updates.

Examples:
- `feat: Speed up compiling with new technique`
- `fix: Fix crash during launch on certain phones`
- `design: Update Design something related UI`

### About pull requests
1. use only english commit message/comment , but you should elaborate on your code.
2. Please follow the code style and design pattern of this project.
3. Welcome everybody take part in this project.

## Known Issues
* Death process restarting produce duplicated activities and process. Temporary solution: kill them all and restart application manually.
* FireFox crashed when inputting website url
* Crashed when getType of content provider calling
* It appeared that an application has multi process of each activity, it should be a process containing activities of an application
* Static broadcast failed test.

## Sponsorship
This project is a free open source project, routine maintenance consumes a lot of time and effort. If you want to speed up the progress or buy the author a cup of coffee.

- BTC: 14z658gFXzNTbGEXySNJLGxwHfJmMViRaB
- USDT（TRC20）: TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK

## Credits
- [VirtualApp](https://github.com/asLody/VirtualApp)
- [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)
- [Pine](https://github.com/canyie/pine)

### License
> ```
> Copyright 2022 NewBlackbox
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
> ```# NewBlackbox
# NewBlackbox