License
=======
Copyright (C) dumptruckman 2013

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at http://mozilla.org/MPL/2.0/.


Maven Dependency Info
=====================
```xml
<repositories>
    <repository>
        <id>onarandombox</id>
        <url>http://repo.onarandombox.com/content/groups/public</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.dumptruckman.minecraft</groupId>
        <artifactId>JsonConfiguration</artifactId>
        <version>1.1</version>
    </dependency>
    <!-- Required json dependency because no other json lib can handle numbers appropriately. -->
    <dependency>
        <groupId>net.minidev</groupId>
        <artifactId>json-smart</artifactId>
        <version>1.1.1</version>
    </dependency>
</dependencies>
```
