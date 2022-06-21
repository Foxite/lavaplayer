import org.gradle.internal.impldep.org.apache.http.impl.client.BasicCredentialsProvider

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
        name = "corsacMaven"
        url = uri("https://maven.repo.corsac.nl/releases")
        credentials(PasswordCredentials::class)
        authentication {
          create<BasicAuthentication>("basic")
        }
      }
    }
  }
}
