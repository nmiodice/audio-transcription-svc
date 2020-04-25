
terraform {
  backend "azurerm" {
    resource_group_name  = "terraform"
    storage_account_name = "tfstatea98db973kw"
    container_name       = "tfstate"
    key                  = "terraform.tfstate"
  }
}


provider "random" {
  version = "2.2.1"
}

provider "azurerm" {
  version = "2.6.0"
  features {}
}

resource "random_string" "slug" {
  length  = 8
  special = false
  upper   = false
  number  = false
}


locals {
  prefix = format("%s%s", var.prefix, random_string.slug.result)
}


resource "azurerm_resource_group" "rg" {
  name     = format("%s-rg", local.prefix)
  location = var.region
}

resource "azurerm_app_service_plan" "asp" {
  name                = format("%s-asp", local.prefix)
  location            = var.region
  resource_group_name = azurerm_resource_group.rg.name
  reserved            = true
  kind                = "Linux"

  sku {
    tier = "Basic"
    size = "B1"
  }
}

resource "azurerm_app_service" "svc" {
  name                = format("%s-app", local.prefix)
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  app_service_plan_id = azurerm_app_service_plan.asp.id
  https_only          = true
  site_config {
    linux_fx_version = format("DOCKER|%s/audio-transcription-service:latest", azurerm_container_registry.acr.login_server)
  }
  app_settings = {
    "AZ_COSMOS_DB"       = azurerm_cosmosdb_sql_database.db.name
    "AZ_COSMOS_ENDPOINT" = azurerm_cosmosdb_account.db.endpoint
    "AZ_COSMOS_KEY"      = azurerm_cosmosdb_account.db.primary_master_key

    "AZ_MEDIA_STORAGE_CONTAINER"   = azurerm_storage_container.container.name
    "AZ_STORAGE_CONNECTION_STRING" = azurerm_storage_account.acct.primary_connection_string

    "AZ_SEARCH_API_ENDPOINT" = format("https://%s.search.windows.net", azurerm_search_service.search.name)
    "AZ_SEARCH_API_INDEX"    = "media-index"
    "AZ_SEARCH_API_KEY"      = azurerm_search_service.search.primary_key

    "AZ_STT_API_KEY" = data.external.stt_keys.result.key1
    "AZ_STT_REGION"  = azurerm_resource_group.rg.location

    "DOCKER_ENABLE_CI"                = "true"
    "DOCKER_REGISTRY_SERVER_PASSWORD" = azurerm_container_registry.acr.admin_password
    "DOCKER_REGISTRY_SERVER_URL"      = azurerm_container_registry.acr.login_server
    "DOCKER_REGISTRY_SERVER_USERNAME" = azurerm_container_registry.acr.admin_username

    "WEBSITES_ENABLE_APP_SERVICE_STORAGE"        = "false"
    "APPINSIGHTS_INSTRUMENTATIONKEY"             = azurerm_application_insights.ai.instrumentation_key
    "ApplicationInsightsAgent_EXTENSION_VERSION" = "~2"

    "WEBSITES_PORT" = "8080"
  }
}

resource "azurerm_application_insights" "ai" {
  name                = format("%s-ai", local.prefix)
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "other"
}

resource "azurerm_cosmosdb_account" "db" {
  name                = format("%s-cosmos", local.prefix)
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  offer_type          = "Standard"
  consistency_policy {
    consistency_level = "Session"
  }
  geo_location {
    location          = azurerm_resource_group.rg.location
    failover_priority = 0
  }
}

resource "azurerm_cosmosdb_sql_database" "db" {
  name                = "media-transcription"
  resource_group_name = azurerm_cosmosdb_account.db.resource_group_name
  account_name        = azurerm_cosmosdb_account.db.name
  throughput          = 400
}

resource "azurerm_container_registry" "acr" {
  name                = format("%sacr", local.prefix)
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  sku                 = "Basic"
  admin_enabled       = true
}

data "external" "webapp_cicd_url" {
  program = [
    "az", "webapp", "deployment", "container", "show-cd-url",
    "-g", azurerm_resource_group.rg.name,
    "-n", azurerm_app_service.svc.name,
    "--query", "{url: CI_CD_URL}"
  ]
}

resource "azurerm_container_registry_webhook" "webhook" {
  name                = "cicdwebhook"
  resource_group_name = azurerm_resource_group.rg.name
  registry_name       = azurerm_container_registry.acr.name
  location            = azurerm_resource_group.rg.location

  service_uri = data.external.webapp_cicd_url.result.url
  status      = "enabled"
  actions     = ["push"]
}


resource "azurerm_storage_account" "acct" {
  name                     = format("%sstorage", local.prefix)
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = azurerm_resource_group.rg.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_storage_container" "container" {
  name                  = format("%s-container", local.prefix)
  storage_account_name  = azurerm_storage_account.acct.name
  container_access_type = "private"
}

resource "null_resource" "stt" {
  provisioner "local-exec" {
    command = format(
      "az cognitiveservices account create -n %s -g %s --kind CognitiveServices --sku S0 -l %s --yes",
      format("%s-stt", local.prefix),
      azurerm_resource_group.rg.name,
      azurerm_resource_group.rg.location
    )
  }
}

data "external" "stt_keys" {
  program = [
    "az", "cognitiveservices", "account", "keys", "list",
    "-g", azurerm_resource_group.rg.name,
    "-n", format("%s-stt", local.prefix)
  ]
}


resource "azurerm_search_service" "search" {
  name                = format("%s-search", local.prefix)
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  sku                 = "free"
}
