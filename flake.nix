{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    flake-parts.url = "github:hercules-ci/flake-parts";
    systems.url = "github:nix-systems/default";
    clj-nix.url = "github:jlesquembre/clj-nix";
  };

  outputs = inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;
      perSystem = { self', pkgs, lib, system, ... }:
        let version = "1.2.1";
        in rec {
          devShells.default = pkgs.mkShell {
            packages = with pkgs; [
              nixfmt-classic
              postgresql
              clojure-lsp
              clojure
              jdk
            ];
          };

          packages.default = inputs.clj-nix.lib.mkCljApp {
            pkgs = pkgs;
            modules = [{
              projectSrc = ./.;
              version = version;
              name = "studerl/breakfastbot";
              main-ns = "breakfastbot.core";
              java-opts = [ "-Dconf=prod-config.edn" ];
              customJdk.enable = true;
            }];
          };

          packages.docker = pkgs.dockerTools.buildLayeredImage {
            name = "studerl/breakfastbot";
            tag = version;
            config.Cmd = [ "${packages.default}/bin/breakfastbot" ];
            contents = with pkgs.dockerTools; [ caCertificates ];
          };
        };
    };
}
