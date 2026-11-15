#!/usr/bin/env python3
"""Run the project's real JUnit tests with Java 21 and locally cached JARs.

Primary command: ./gradlew clean test build
Fallback: python3 scripts/verify-offline.py [Gradle modules-2/files-2.1 directory]
This script does not download dependencies or modify Gradle's cache/configuration.
"""
from pathlib import Path
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / '.gradle/caches/modules-2/files-2.1'
OUT = ROOT / 'build/offline-tests'
ARTIFACTS = [
    ('org.junit.jupiter', 'junit-jupiter-api', '5.11.4'),
    ('org.junit.jupiter', 'junit-jupiter-params', '5.11.4'),
    ('org.junit.jupiter', 'junit-jupiter-engine', '5.11.4'),
    ('org.junit.platform', 'junit-platform-commons', '1.11.4'),
    ('org.junit.platform', 'junit-platform-engine', '1.11.4'),
    ('org.junit.platform', 'junit-platform-launcher', '1.11.4'),
    ('org.opentest4j', 'opentest4j', '1.3.0'),
    ('org.apiguardian', 'apiguardian-api', '1.1.2'),
]


def run(args):
    print('+', ' '.join(str(arg) for arg in args), flush=True)
    subprocess.run([str(arg) for arg in args], cwd=ROOT, check=True)


def main():
    jars = []
    for group, artifact, version in ARTIFACTS:
        matches = sorted((CACHE / group / artifact / version).glob(f'*/{artifact}-{version}.jar'))
        if len(matches) != 1:
            raise SystemExit(f'Expected one cached {artifact}:{version} JAR, found {len(matches)}. Run Gradle with dependencies available.')
        jars.append(matches[0])
    OUT.mkdir(parents=True, exist_ok=True)
    # Clean only this script's generated output to avoid executing stale test classes.
    import shutil
    for name in ('main', 'test', 'runner'):
        folder = OUT / name
        if folder.exists():
            shutil.rmtree(folder)
        folder.mkdir()
    classpath = os.pathsep.join(str(path) for path in jars)
    run(['java', '-version'])
    run(['javac', '--release', '21', '-encoding', 'UTF-8', '-d', OUT / 'main',
         *sorted((ROOT / 'src/main/java').rglob('*.java'))])
    classpath += os.pathsep + str(OUT / 'main')
    run(['javac', '--release', '21', '-encoding', 'UTF-8', '-cp', classpath, '-d', OUT / 'test',
         *sorted((ROOT / 'src/test/java').rglob('*.java'))])
    launcher = OUT / 'runner/OfflineTestLauncher.java'
    launcher.write_text('''
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public class OfflineTestLauncher {
  public static void main(String[] args) {
    if (!System.getProperty("java.specification.version").equals("21")) {
      throw new IllegalStateException("Verification requires Java 21");
    }
    var listener = new SummaryGeneratingListener();
    var launcher = LauncherFactory.create();
    var counts = new java.util.TreeMap<String, long[]>();
    launcher.registerTestExecutionListeners(listener, new org.junit.platform.launcher.TestExecutionListener() {
      @Override public synchronized void executionFinished(org.junit.platform.launcher.TestIdentifier test,
          org.junit.platform.engine.TestExecutionResult result) {
        if (!test.isTest()) return;
        test.getSource().filter(org.junit.platform.engine.support.descriptor.MethodSource.class::isInstance)
            .map(org.junit.platform.engine.support.descriptor.MethodSource.class::cast).ifPresent(source -> {
              long[] count = counts.computeIfAbsent(source.getClassName(), key -> new long[3]);
              int index = switch (result.getStatus()) { case SUCCESSFUL -> 0; case FAILED -> 1; case ABORTED -> 2; };
              count[index]++;
            });
      }
    });
    launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectPackage("edu.hoteluq")).build());
    var summary = listener.getSummary();
    var writer = new java.io.PrintWriter(System.out, true);
    summary.printTo(writer);
    summary.printFailuresTo(writer);
    writer.println("Results by class (successful / failed / aborted):");
    counts.forEach((name, count) -> writer.println(name + ": " + count[0] + " / " + count[1] + " / " + count[2]));
    if (summary.getTestsFoundCount() == 0 || summary.getTestsFailedCount() != 0
        || summary.getContainersFailedCount() != 0 || summary.getTestsSkippedCount() != 0
        || summary.getTestsAbortedCount() != 0) System.exit(1);
  }
}
''', encoding='utf-8')
    run(['javac', '--release', '21', '-cp', classpath, '-d', OUT / 'runner', launcher])
    classpath += os.pathsep + str(OUT / 'test') + os.pathsep + str(OUT / 'runner')
    run(['java', '-cp', classpath, 'OfflineTestLauncher'])


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
