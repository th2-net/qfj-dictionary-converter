# qfj-dictionary-converter

The CLI tool to convert dictionaries in QuickFIXJ (QFJ) format into Sailfish format.

## Usage

Extract files from the archive and execute the `convert` command

```bash
./bin/cli-qfj-dictionary-converter convert -i <directory with QFJ dictionaries> -o <directory to store output dictionaries>
```

To validate the resulting dictionaries execute the `validate` command

```bash
./bin/cli-qfj-dictionary-converter validate -d <directory with Sailfish dictionaries>
```

You can chain commands

```bash
./bin/cli-qfj-dictionary-converter convert \
    -i <directory with QFJ dictionaries> \
    -o <directory to store output dictionaries> \
    validate \
    -d <directory to store output dictionaries>
```

You can always use option `-h`/`--help` to see the help for the command.

## Build

Execute the following command to get a local installation

For Linux:

```bash
./gradlew clean installDist
```

For Windows:

```powershell
gradlew.bat clean installDist
```

The installed application is localed here: `build/install/cli-qfj-dictionary-converter/`