#!/usr/bin/env python
import fnmatch, os, sys, subprocess

def run(cmd):
	try:
		p = subprocess.Popen(cmd)
		(out, err) = p.communicate()

		if out:
			print('out: %s' % out.decode(sys.stdout.encoding or 'iso8859-1'))

		if err:
			print('err: %s' % err.decode(sys.stdout.encoding or 'iso8859-1'))

		return p.returncode
	except OSError:
		return -1

def matchFiles(path, pattern):
	paths = []
	for root, dirnames, filenames in os.walk(path):
		for filename in fnmatch.filter(filenames, pattern):
			paths.append(os.path.join(root, filename))
	return paths

jarFiles = matchFiles('libs/', '*.jar')

if not os.path.exists("build/"):
	os.makedirs("build/")

for app in ["oopsc", "oopsvm"]:
	javaFiles = matchFiles('src/org/' + app, '*.java')

	scalaFiles = matchFiles('src/org/' + app, '*.scala')
	scalaFiles = [file for file in scalaFiles if "TestSuite.scala" not in file]

	srcFiles = javaFiles + scalaFiles

	cmdCompile = ["/usr/bin/scalac", "-cp", ":".join(jarFiles), "-d", "build/"] + srcFiles
	print(" ".join(cmdCompile))
	run(cmdCompile)

	cmdCompile = ["/usr/bin/javac", "-cp", ":".join(jarFiles) + ":build/", "-d", "build/"] + javaFiles
	print(" ".join(cmdCompile))
	run(cmdCompile)

	classFiles = []
	for file in matchFiles('build/org/' + app, '*.class'):
		classFiles.append("-C")
		classFiles.append("build/")
		classFiles.append(file.replace("build/", ""))

	mainClass = "org." + app + "." + app.upper()

	cmdPackage = ["/usr/bin/jar", "cvmfe", "Manifest.txt", app + ".jar", mainClass] + classFiles
	print(" ".join(cmdPackage))
	run(cmdPackage)
