plugins {
  java
  `maven-publish`
}

group = "dev.foxite"

allprojects {
  group = rootProject.group

  repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
  }

  apply(plugin = "java")
  apply(plugin = "maven-publish")

  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  publishing {
    repositories {
      maven {
        setUrl("https://maven.repo.corsac.nl")
        //credentials(AwsCredentials::class) {
        //  accessKey = project.findProperty("sedmelluqMavenS3AccessKey")?.toString()
        //  secretKey = project.findProperty("sedmelluqMavenS3SecretKey")?.toString()
        //}
      }
    }
  }
}
