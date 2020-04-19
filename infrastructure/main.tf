
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
  length = 8
  special = false
  upper = false
  number = false
}


locals {
    prefix = format("%s%s", var.prefix, random_string.slug.result)
}

resource "azurerm_resource_group" "rg" {
  name     = format("%s-rg", local.prefix)
  location = var.region
}


##
# App Service
##
resource "azurerm_app_service_plan" "asp" {
  name                = format("%s-asp", local.prefix)
  location            = var.region
  resource_group_name = azurerm_resource_group.rg.name
  reserved = true
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
}


##
# Cosmos
##
# resource "azurerm_cosmosdb_account" "db" {
#   name                = format("%s-cosmos", local.prefix)
#   location            = azurerm_resource_group.rg.location
#   resource_group_name = azurerm_resource_group.rg.name
#   offer_type          = "Standard"
#   consistency_policy {
#     consistency_level       = "session"
#     max_interval_in_seconds = 10
#     max_staleness_prefix    = 200
#   }
# }
